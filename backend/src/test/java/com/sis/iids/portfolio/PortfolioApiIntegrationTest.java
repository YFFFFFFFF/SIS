package com.sis.iids.portfolio;

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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-13 投资组合优化 API 集成测试（FR-03-02）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class PortfolioApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.sis.iids.calculation.CalculationService calculationService;

    @Test
    void optimizesAndPersistsRun() throws Exception {
        // 两个已测算方案：高收益（price=140）与低收益（price=90）
        createCalculatedScenario("SIS-R13-HI", 140);
        createCalculatedScenario("SIS-R13-LO", 90);

        String resp = mockMvc.perform(post("/api/v1/portfolio-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":250000,\"maxCount\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId", notNullValue()))
                .andExpect(jsonPath("$.data.candidateCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.totalNpv", notNullValue()))
                .andExpect(jsonPath("$.data.explanation", containsString("资金利用率")))
                .andExpect(jsonPath("$.data.frontier", hasSize(21)))
                .andExpect(jsonPath("$.data.members.length()", greaterThanOrEqualTo(2)))
                .andReturn().getResponse().getContentAsString();
        Long runId = extractRunId(resp);

        mockMvc.perform(get("/api/v1/portfolio-runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.budget", notNullValue()))
                .andExpect(jsonPath("$.data.members.length()", greaterThanOrEqualTo(2)));
    }

    @Test
    void maxCountLimitsSelection() throws Exception {
        createCalculatedScenario("SIS-R13-A", 140);
        createCalculatedScenario("SIS-R13-B", 120);

        mockMvc.perform(post("/api/v1/portfolio-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":1000000,\"maxCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members[0].selected").value(true))
                .andExpect(jsonPath("$.data.members[1].selected").value(false));
    }

    @Test
    void rejectsNonPositiveBudget() throws Exception {
        mockMvc.perform(post("/api/v1/portfolio-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":0}"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // fixtures
    // ============================================================
    private Long createCalculatedScenario(String code, int price) throws Exception {
        Long projectId = createProject(code);
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"组合候选-" + code + "\",\"horizonYears\":5,\"constructionYears\":1}"))
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

    private Long extractRunId(String response) {
        return Long.valueOf(response.replaceAll("(?s).*\\\"runId\\\":(\\d+).*", "$1"));
    }
}
