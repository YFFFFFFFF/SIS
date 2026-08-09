package com.sis.iids.sensitivity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-04 敏感性分析 API 集成测试（FR-02-01）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class SensitivityApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void runsTwoFactorSensitivityAndPersists() throws Exception {
        Long scenarioId = createBaseScenario();

        String resp = mockMvc.perform(post("/api/v1/scenarios/{id}/sensitivity", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"NPV","variable1":"PRICE","range1":0.20,"steps1":5,
                                 "variable2":"UNIT_COST","range2":0.20,"steps2":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId", notNullValue()))
                .andExpect(jsonPath("$.data.baseValue", notNullValue()))
                .andExpect(jsonPath("$.data.matrix", hasSize(25)))
                .andExpect(jsonPath("$.data.coefficient1", notNullValue()))
                .andExpect(jsonPath("$.data.coefficient2", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        Long runId = extractRunId(resp);

        mockMvc.perform(get("/api/v1/sensitivity-runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matrix", hasSize(25)))
                .andExpect(jsonPath("$.data.variable1").value("PRICE"))
                .andExpect(jsonPath("$.data.variable2").value("UNIT_COST"));

        mockMvc.perform(get("/api/v1/scenarios/{id}/sensitivity", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void runsSingleFactorSensitivity() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(post("/api/v1/scenarios/{id}/sensitivity", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variable1\":\"INVESTMENT\",\"range1\":0.20,\"steps1\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matrix", hasSize(5)))
                .andExpect(jsonPath("$.data.variable2").doesNotExist());
    }

    @Test
    void rejectsInvalidVariable() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(post("/api/v1/scenarios/{id}/sensitivity", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variable1\":\"NOPE\",\"range1\":0.20,\"steps1\":5}"))
                .andExpect(status().isBadRequest());
    }

    private Long createBaseScenario() throws Exception {
        Long projectId = createProject("SIS-R04-SENS");
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"敏感性基准\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long scenarioId = extractId(scenarioResp);
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                                 "pricePerUnit":140,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CONSTRUCTION\",\"name\":\"建设投资\",\"amount\":200000,\"yearNo\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"WORKING_CAPITAL\",\"name\":\"流动资金\",\"amount\":20000,\"yearNo\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scenarios/{id}/financing-plans", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"EQUITY\",\"ratio\":1,\"amount\":220000,\"interestRate\":0,\"termYears\":0}"))
                .andExpect(status().isOk());
        return scenarioId;
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
        return Long.valueOf(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }

    private Long extractRunId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"runId\\\":(\\d+).*", "$1"));
    }
}
