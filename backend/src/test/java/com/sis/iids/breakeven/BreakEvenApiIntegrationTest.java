package com.sis.iids.breakeven;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-10 盈亏平衡分析 API 集成测试（FR-02-02）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class BreakEvenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsThreeBreakEvenMeasures() throws Exception {
        Long scenarioId = createBaseScenario();

        mockMvc.perform(get("/api/v1/scenarios/{id}/break-even", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.solvable").value(true))
                .andExpect(jsonPath("$.data.contributionMargin", closeTo(100.0, 0.01)))
                // 固定成本 = 10000 + 折旧 40000 = 50000
                .andExpect(jsonPath("$.data.annualFixedCost", closeTo(50000.0, 1.0)))
                .andExpect(jsonPath("$.data.bepOutput", closeTo(500.0, 1.0)))
                .andExpect(jsonPath("$.data.bepUtilization", closeTo(0.5, 0.01)))
                .andExpect(jsonPath("$.data.bepPrice", closeTo(90.0, 0.5)))
                .andExpect(jsonPath("$.data.curve", hasSize(11)))
                .andExpect(jsonPath("$.data.assumptionNote", notNullValue()));
    }

    @Test
    void unsolvableWhenPriceBelowVariableCost() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                                 "pricePerUnit":30,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/scenarios/{id}/break-even", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.solvable").value(false))
                .andExpect(jsonPath("$.data.unsolvableReason", notNullValue()));
    }

    @Test
    void scenarioNotFoundReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios/{id}/break-even", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private Long createBaseScenario() throws Exception {
        Long projectId = createProject("SIS-R10-BEP");
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"盈亏平衡基准\",\"horizonYears\":5,\"constructionYears\":1}"))
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
        return Long.valueOf(response.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1"));
    }
}
