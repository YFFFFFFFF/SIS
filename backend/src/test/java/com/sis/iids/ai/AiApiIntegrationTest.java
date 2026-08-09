package com.sis.iids.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-17 AI 决策引擎 API 集成测试（FR-05）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class AiApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.sis.iids.calculation.CalculationService calculationService;

    @Test
    void operationRecordsRoundTrip() throws Exception {
        Long projectId = createProject("SIS-R17-OP");
        mockMvc.perform(post("/api/v1/projects/{id}/ai/operation-records", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period":"2025","actualRevenue":130000,"actualCost":52000,"actualNpv":75000,
                                 "actualIrr":0.21,"deviationRatio":-0.12,"verified":true,"note":"首年运营数据"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.verified").value(true));

        mockMvc.perform(get("/api/v1/projects/{id}/ai/operation-records", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].period").value("2025"));
    }

    @Test
    void paramRecommendationWithBasis() throws Exception {
        Long scenarioId = createCalculatedScenario("SIS-R17-REC", 140);
        mockMvc.perform(get("/api/v1/scenarios/{id}/ai/param-recommendation", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data.items[0].param").value("wacc"))
                .andExpect(jsonPath("$.data.items[0].basis", notNullValue()))
                .andExpect(jsonPath("$.data.basisSummary", containsString("依据")));
    }

    @Test
    void scoresScenarioWithFactors() throws Exception {
        Long scenarioId = createCalculatedScenario("SIS-R17-SCORE", 140);
        mockMvc.perform(get("/api/v1/scenarios/{id}/ai/score", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelCode").value("SCORING_V1"))
                .andExpect(jsonPath("$.data.totalScore", greaterThan(60.0)))
                .andExpect(jsonPath("$.data.label", notNullValue()))
                .andExpect(jsonPath("$.data.disclaimer", containsString("不替代人工决策")))
                .andExpect(jsonPath("$.data.factors", hasSize(6)))
                .andExpect(jsonPath("$.data.factors[0].explain", notNullValue()));
    }

    @Test
    void scoreWithoutCalculationReturns400() throws Exception {
        Long projectId = createProject("SIS-R17-NOCALC");
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"未测算\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long scenarioId = extractId(scenarioResp);
        mockMvc.perform(get("/api/v1/scenarios/{id}/ai/score", scenarioId))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // fixtures
    // ============================================================
    private Long createCalculatedScenario(String code, int price) throws Exception {
        Long projectId = createProject(code);
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"AI 打分方案\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long scenarioId = extractId(scenarioResp);
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                                 "pricePerUnit":%d,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000}
                                """).formatted(price)))
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
        String taskResp = mockMvc.perform(post("/api/v1/scenarios/{id}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"taskType\":\"FULL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        calculationService.executeTask(extractId(taskResp));
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
        return Long.valueOf(response.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1"));
    }
}
