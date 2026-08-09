package com.sis.iids.collab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-15c 字段锁强制拦截 + 变更留痕集成测试（FR-04-02）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class FieldLockEnforcementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String PARAM_BODY = "{\"wacc\":%s,\"taxRate\":0.25,\"depreciationYears\":20,\"residualRate\":0.05,"
            + "\"loanRatioLimit\":0.7,\"pricePerUnit\":0.42,\"unitCost\":0.18,\"annualOutput\":128000000,\"fixedOperatingCost\":500000}";

    @Test
    void paramSaveBlockedByOthersLockAndAllowedForHolder() throws Exception {
        Long scenarioId = createScenario("SIS-R15C-P1");
        upsertParams(scenarioId, "0.08");

        // 他人锁定 param.wacc（模拟另一用户持有）
        lockField(scenarioId, "param.wacc", 2, "finance_specialist");

        // 当前用户(admin)改 wacc → 409，提示被 finance_specialist 锁
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PARAM_BODY.formatted("0.09")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("param.wacc")))
                .andExpect(jsonPath("$.message", containsString("finance_specialist")));

        // 锁持有人本人改 wacc → 200（先以持有人身份断言可通过：用 holderName=admin 的锁覆盖）
        releaseField(scenarioId, "param.wacc", 2, "finance_specialist");
        lockField(scenarioId, "param.wacc", 1, "admin");
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PARAM_BODY.formatted("0.09")))
                .andExpect(status().isOk());
    }

    @Test
    void unchangedLockedFieldDoesNotBlockSave() throws Exception {
        Long scenarioId = createScenario("SIS-R15C-P2");
        upsertParams(scenarioId, "0.08");

        // 他人锁定 param.wacc；本次保存不改 wacc（仍传 0.08）→ 不触发锁
        lockField(scenarioId, "param.wacc", 2, "finance_specialist");
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PARAM_BODY.formatted("0.08")))
                .andExpect(status().isOk());
    }

    @Test
    void paramSaveRecordsFieldUpdatedChanges() throws Exception {
        Long scenarioId = createScenario("SIS-R15C-P3");
        upsertParams(scenarioId, "0.08");
        upsertParams(scenarioId, "0.10"); // 改 wacc

        // 变更时间线应含 param.wacc 的 FIELD_UPDATED（old 0.08 → new 0.1）
        mockMvc.perform(get("/api/v1/scenarios/{id}/changes", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.changeType=='FIELD_UPDATED')].fieldName", hasItem("param.wacc")))
                .andExpect(jsonPath("$.data[?(@.fieldName=='param.wacc')].newValue", hasItem("0.1")));

        // 协同目录的 param.wacc 行应显示最后编辑人 admin
        mockMvc.perform(get("/api/v1/scenarios/{id}/collab/fields", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.fieldKey=='param.wacc')].lastEditor", hasItem("admin")));
    }

    @Test
    void investmentAmountUpdateBlockedByLockAndLogged() throws Exception {
        Long scenarioId = createScenario("SIS-R15C-I1");
        Long itemId = createInvestmentItem(scenarioId);

        // 他人锁定该分项金额字段
        lockField(scenarioId, "investment.amount:" + itemId, 2, "technical_engineer");

        // 改金额 → 409
        mockMvc.perform(put("/api/v1/scenarios/{sid}/investment-items/{iid}", scenarioId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION\",\"name\":\"厂房\",\"amount\":20000,\"yearNo\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("investment.amount:" + itemId)));

        // 释放后改金额为 30000（与当前 20000 不同，确保产生 FIELD_UPDATED 留痕）
        releaseField(scenarioId, "investment.amount:" + itemId, 2, "technical_engineer");
        mockMvc.perform(put("/api/v1/scenarios/{sid}/investment-items/{iid}", scenarioId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION\",\"name\":\"厂房\",\"amount\":30000,\"yearNo\":1}"))
                .andExpect(status().isOk());

        var changesResp = mockMvc.perform(get("/api/v1/scenarios/{id}/changes", scenarioId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // 目标行：fieldName=investment.amount:{itemId} 且 changeType=FIELD_UPDATED
        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(changesResp);
        java.util.List<com.fasterxml.jackson.databind.JsonNode> matched = new java.util.ArrayList<>();
        root.path("data").forEach(n -> {
            if (java.util.Objects.equals(n.path("fieldName").asText(null), "investment.amount:" + itemId)
                    && "FIELD_UPDATED".equals(n.path("changeType").asText())) {
                matched.add(n);
            }
        });
        org.junit.jupiter.api.Assertions.assertFalse(matched.isEmpty(), "应存在 investment.amount 的 FIELD_UPDATED 留痕，实际：" + changesResp);
        org.junit.jupiter.api.Assertions.assertEquals("20000", matched.get(0).path("oldValue").asText());
        org.junit.jupiter.api.Assertions.assertEquals("30000", matched.get(0).path("newValue").asText());
    }

    @Test
    void createItemRecordsTimeline() throws Exception {
        Long scenarioId = createScenario("SIS-R15C-C1");
        createInvestmentItem(scenarioId);

        mockMvc.perform(get("/api/v1/scenarios/{id}/changes", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.changeType=='FIELD_UPDATED')].newValue",
                        hasItem(containsString("新增投资项目"))));
    }

    // ----------------------------------------------------------
    private void upsertParams(Long scenarioId, String wacc) throws Exception {
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PARAM_BODY.formatted(wacc)))
                .andExpect(status().isOk());
    }

    private void lockField(Long scenarioId, String fieldKey, long holderId, String holderName) throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + fieldKey + "\",\"holderId\":" + holderId
                                + ",\"holderName\":\"" + holderName + "\",\"ttlMinutes\":30}"))
                .andExpect(status().isOk());
    }

    private void releaseField(Long scenarioId, String fieldKey, long holderId, String holderName) throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks/release", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + fieldKey + "\",\"holderId\":" + holderId
                                + ",\"holderName\":\"" + holderName + "\"}"))
                .andExpect(status().isOk());
    }

    private Long createInvestmentItem(Long scenarioId) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION\",\"name\":\"厂房\",\"amount\":10000,\"yearNo\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(resp.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1"));
    }

    private Long createScenario(String code) throws Exception {
        Long projectId = createProject(code);
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"强制拦截场景\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(scenarioResp);
    }

    private Long createProject(String code) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"Host\",\"projectType\":\"INDUSTRIAL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(resp);
    }

    private Long extractId(String response) {
        return Long.valueOf(response.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1"));
    }
}
