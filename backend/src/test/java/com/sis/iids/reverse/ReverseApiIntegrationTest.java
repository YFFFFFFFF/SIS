package com.sis.iids.reverse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-09 目标反算 API 集成测试（FR-02-02）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class ReverseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void runsReverseSolveAndPersists() throws Exception {
        Long scenarioId = createBaseScenario();

        String resp = mockMvc.perform(post("/api/v1/scenarios/{id}/reverse-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"NPV","targetValue":0,"variable":"PRICE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId", notNullValue()))
                .andExpect(jsonPath("$.data.feasible").value(true))
                .andExpect(jsonPath("$.data.solvedValue", lessThan(140.0)))
                .andExpect(jsonPath("$.data.factor", notNullValue()))
                .andExpect(jsonPath("$.data.achievedValue", notNullValue()))
                .andExpect(jsonPath("$.data.sensitivityNote", notNullValue()))
                .andExpect(jsonPath("$.data.boundaryNote", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        Long runId = extractRunId(resp);

        mockMvc.perform(get("/api/v1/reverse-runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetMetric").value("NPV"))
                .andExpect(jsonPath("$.data.variable").value("PRICE"));

        mockMvc.perform(get("/api/v1/scenarios/{id}/reverse-runs", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void solvesInvestmentForIrrTarget() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(post("/api/v1/scenarios/{id}/reverse-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"IRR","targetValue":0.15,"variable":"INVESTMENT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feasible").value(true))
                .andExpect(jsonPath("$.data.solvedValue", notNullValue()));
    }

    @Test
    void rejectsInvalidVariable() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(post("/api/v1/scenarios/{id}/reverse-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMetric\":\"NPV\",\"targetValue\":0,\"variable\":\"NOPE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidTargetMetric() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(post("/api/v1/scenarios/{id}/reverse-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMetric\":\"NOPE\",\"targetValue\":0,\"variable\":\"PRICE\"}"))
                .andExpect(status().isBadRequest());
    }

    private Long createBaseScenario() throws Exception {
        Long projectId = createProject("SIS-R09-REV");
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"反算基准\",\"horizonYears\":5,\"constructionYears\":1}"))
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
        return Long.valueOf(response.replaceAll("(?s).*\\\"runId\\\":(\\d+).*", "$1"));
    }
}
