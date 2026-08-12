package com.sis.iids.bpm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-14 BPM 可配置审批流 API 集成测试（FR-04-03）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class BpmApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultFlowSeeded() throws Exception {
        mockMvc.perform(get("/api/v1/admin/approval-flows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].code").value("DEFAULT_REVIEW_CHAIN"))
                .andExpect(jsonPath("$.data[0].isDefault").value(true))
                .andExpect(jsonPath("$.data[0].nodes", hasSize(2)))
                .andExpect(jsonPath("$.data[0].nodes[0].nodeCode").value("REVIEW"))
                .andExpect(jsonPath("$.data[0].nodes[1].nodeCode").value("APPROVAL"));
    }

    @Test
    void flowMutationIsFrozenDuringUserTesting() throws Exception {
        mockMvc.perform(post("/api/v1/admin/approval-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"COMMITTEE_CHAIN","name":"投委会审批链","description":"财务复核→投委会→总经理",
                                 "nodes":[
                                   {"nodeCode":"REVIEW","nodeName":"财务复核","seq":1,"approverRole":"FINANCE_SPECIALIST"},
                                   {"nodeCode":"COMMITTEE","nodeName":"投资委员会","seq":2,"approverRole":"INVESTMENT_COMMITTEE","conditionExpr":"参数调整 >±5% 升级"},
                                   {"nodeCode":"GM","nodeName":"总经理审批","seq":3,"approverRole":"GENERAL_MANAGER"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("固定流程")));
    }

    @Test
    void frozenFlowRejectsAllCreateRequestsConsistently() throws Exception {
        mockMvc.perform(post("/api/v1/admin/approval-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DEFAULT_REVIEW_CHAIN","name":"重复",
                                 "nodes":[{"nodeCode":"A","nodeName":"A","seq":1,"approverRole":"X"}]}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/approval-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"BAD_SEQ","name":"坏顺序",
                                 "nodes":[{"nodeCode":"A","nodeName":"A","seq":2,"approverRole":"X"}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void timelineTracksInstanceProgress() throws Exception {
        // 建方案 → 提交审批 → 财务复核通过 → 时间线校验
        Long scenarioId = createScenario("SIS-R14-TL");
        String submitResp = mockMvc.perform(post("/api/v1/scenarios/{id}/approval/submit", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long instanceId = extractId(submitResp);

        mockMvc.perform(get("/api/v1/approval-instances/{id}/timeline", instanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flowCode").value("DEFAULT_REVIEW_CHAIN"))
                .andExpect(jsonPath("$.data.flowNodes", hasSize(2)))
                .andExpect(jsonPath("$.data.flowNodes[0].current").value(true))
                .andExpect(jsonPath("$.data.flowNodes[0].passed").value(false))
                .andExpect(jsonPath("$.data.events", hasSize(1)));

        mockMvc.perform(post("/api/v1/approval-instances/{id}/review/approve", instanceId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/approval-instances/{id}/timeline", instanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flowNodes[0].passed").value(true))
                .andExpect(jsonPath("$.data.flowNodes[1].current").value(true))
                .andExpect(jsonPath("$.data.events", hasSize(2)));
    }

    @Test
    void defaultFlowCannotBeDeleted() throws Exception {
        String resp = mockMvc.perform(get("/api/v1/admin/approval-flows"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long defaultId = extractId(resp);
        mockMvc.perform(delete("/api/v1/admin/approval-flows/{id}", defaultId))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // fixtures
    // ============================================================
    private Long createScenario(String code) throws Exception {
        Long projectId = createProject(code);
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"审批流场景\",\"horizonYears\":5,\"constructionYears\":1}"))
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
        return Long.valueOf(response.replaceAll("(?s).*?\\\"id\\\":(\\d+).*", "$1"));
    }
}
