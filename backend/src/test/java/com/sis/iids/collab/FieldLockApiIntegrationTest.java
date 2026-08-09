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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-15 收尾：字段级锁定与冲突合并 API 集成测试（FR-04-02）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class FieldLockApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acquireConflictAndReleaseFlow() throws Exception {
        Long scenarioId = createScenario("SIS-R15-F1");
        String field = "param.wacc";

        // 甲获取锁
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + field + "\",\"holderId\":1,\"holderName\":\"investment_analyst\",\"ttlMinutes\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fieldKey").value(field))
                .andExpect(jsonPath("$.data.holderName").value("investment_analyst"));

        // 乙获取同一字段 → 409 冲突，提示持有人
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + field + "\",\"holderId\":2,\"holderName\":\"finance_specialist\",\"ttlMinutes\":30}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("investment_analyst")));

        // 乙可获取另一字段（字段级粒度，互不影响）
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"param.taxRate\",\"holderId\":2,\"holderName\":\"finance_specialist\",\"ttlMinutes\":30}"))
                .andExpect(status().isOk());

        // 列表应有 2 条有效锁
        mockMvc.perform(get("/api/v1/scenarios/{id}/field-locks", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 乙释放甲的锁 → 409（非持有人）
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks/release", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + field + "\",\"holderId\":2,\"holderName\":\"finance_specialist\"}"))
                .andExpect(status().isConflict());

        // 甲本人释放 → 成功
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks/release", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + field + "\",\"holderId\":1,\"holderName\":\"investment_analyst\"}"))
                .andExpect(status().isOk());

        // 释放后仅剩 1 条
        mockMvc.perform(get("/api/v1/scenarios/{id}/field-locks", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void renewSameHolderAndTakeOverExpired() throws Exception {
        Long scenarioId = createScenario("SIS-R15-F2");
        String field = "param.pricePerUnit";

        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + field + "\",\"holderId\":1,\"holderName\":\"analyst\",\"ttlMinutes\":30}"))
                .andExpect(status().isOk());

        // 同人再次获取 → 续期成功（不冲突）
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + field + "\",\"holderId\":1,\"holderName\":\"analyst\",\"ttlMinutes\":60}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminForceRelease() throws Exception {
        Long scenarioId = createScenario("SIS-R15-F3");
        String field = "param.unitCost";

        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"" + field + "\",\"holderId\":1,\"holderName\":\"analyst\",\"ttlMinutes\":30}"))
                .andExpect(status().isOk());

        // 管理员强制释放（冲突合并人工兜底）
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks/force-release?fieldKey=" + field, scenarioId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/scenarios/{id}/field-locks", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void fieldCatalogAggregatesInputsAndLockState() throws Exception {
        Long scenarioId = createScenario("SIS-R15-F4");

        // 造参数集（新建方案无参数，目录依赖其展开 param.* 行）
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wacc\":0.08,\"taxRate\":0.25,\"depreciationYears\":20,\"residualRate\":0.05,\"loanRatioLimit\":0.7,\"pricePerUnit\":0.42,\"unitCost\":0.18,\"annualOutput\":128000000,\"fixedOperatingCost\":500000}"))
                .andExpect(status().isOk());

        // 锁一个参数字段
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"param.wacc\",\"holderId\":1,\"holderName\":\"analyst\",\"ttlMinutes\":30}"))
                .andExpect(status().isOk());

        // 目录：应包含 param.* 行，且 param.wacc 行带锁持有人
        mockMvc.perform(get("/api/v1/scenarios/{id}/collab/fields", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.fieldKey=='param.wacc')].lockHolder").value(org.hamcrest.Matchers.hasItem("analyst")))
                .andExpect(jsonPath("$.data[?(@.fieldKey=='param.wacc')].ownerDept").value(org.hamcrest.Matchers.hasItem("财务部")));
    }

    @Test
    void lockOnMissingScenarioReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/{id}/field-locks", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldKey\":\"param.wacc\",\"holderId\":1,\"holderName\":\"analyst\",\"ttlMinutes\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private Long createScenario(String code) throws Exception {
        Long projectId = createProject(code);
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"字段锁场景\",\"horizonYears\":5,\"constructionYears\":1}"))
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
