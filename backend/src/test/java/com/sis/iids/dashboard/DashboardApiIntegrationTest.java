package com.sis.iids.dashboard;

import com.sis.iids.worker.CalculationWorker;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-07 BI 仪表盘聚合接口集成测试（FR-04-01）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class DashboardApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalculationWorker calculationWorker;

    @Test
    void summaryAggregatesKpisBubblesAndStages() throws Exception {
        Long projectId = createProject("SIS-R07-DASH");
        Long scenarioId = createScenario(projectId, "看板方案A");
        setupCalculatedScenario(scenarioId, 140, "dash-a");
        Long scenarioB = createScenario(projectId, "看板方案B");
        setupCalculatedScenario(scenarioB, 180, "dash-b");

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kpis.projectCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.kpis.totalNpv", notNullValue()))
                .andExpect(jsonPath("$.data.kpis.weightedIrr", notNullValue()))
                .andExpect(jsonPath("$.data.bubbles", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.stageCounts", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.industryAmounts", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.riskSignals", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.todos", hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    void emptyPortfolioReturnsZeroKpisWithPlaceholderSignal() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kpis.projectCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.riskSignals", hasSize(greaterThanOrEqualTo(1))));
    }

    private void setupCalculatedScenario(Long scenarioId, int price, String requestKey) throws Exception {
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                                 "pricePerUnit":%d,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000}
                                """.formatted(price)))
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
        mockMvc.perform(post("/api/v1/scenarios/{id}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskType\":\"FINANCIAL\",\"requestKey\":\"" + requestKey + "\"}"))
                .andExpect(status().isOk());
        calculationWorker.runPendingOnce();
    }

    private Long createProject(String code) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"看板宿主项目\",\"projectType\":\"INDUSTRIAL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(resp);
    }

    private Long createScenario(Long projectId, String name) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractId(resp);
    }

    private Long extractId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }
}
