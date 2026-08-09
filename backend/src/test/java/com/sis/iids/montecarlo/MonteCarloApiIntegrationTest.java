package com.sis.iids.montecarlo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.closeTo;
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
 * R-11 蒙特卡洛概率分析 API 集成测试（FR-02-03）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class MonteCarloApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void runsMonteCarloAndPersistsSeed() throws Exception {
        Long scenarioId = createBaseScenario();

        String resp = mockMvc.perform(post("/api/v1/scenarios/{id}/monte-carlo-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"NPV","iterations":2000,"seed":42,
                                 "variables":[{"variable":"PRICE","type":"TRIANGULAR","min":-0.2,"mode":0,"max":0.2}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId", notNullValue()))
                .andExpect(jsonPath("$.data.seed").value(42))
                .andExpect(jsonPath("$.data.iterations").value(2000))
                .andExpect(jsonPath("$.data.mean", closeTo(88022.58, 20000.0)))
                .andExpect(jsonPath("$.data.probPositive", greaterThan(0.9)))
                .andExpect(jsonPath("$.data.var95", notNullValue()))
                .andExpect(jsonPath("$.data.histogram", hasSize(20)))
                .andExpect(jsonPath("$.data.cumulative", hasSize(21)))
                .andReturn().getResponse().getContentAsString();
        Long runId = extractRunId(resp);
        String mean1 = resp.replaceAll("(?s).*\"mean\":(-?[0-9.]+).*", "$1");

        // 同种子复算可复现（红线 R11）：均值必须完全一致
        String resp2 = mockMvc.perform(post("/api/v1/scenarios/{id}/monte-carlo-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"NPV","iterations":2000,"seed":42,
                                 "variables":[{"variable":"PRICE","type":"TRIANGULAR","min":-0.2,"mode":0,"max":0.2}]}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String mean2 = resp2.replaceAll("(?s).*\"mean\":(-?[0-9.]+).*", "$1");
        org.assertj.core.api.Assertions.assertThat(mean2).isEqualTo(mean1);

        mockMvc.perform(get("/api/v1/monte-carlo-runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetMetric").value("NPV"))
                .andExpect(jsonPath("$.data.variables", hasSize(1)));

        mockMvc.perform(get("/api/v1/scenarios/{id}/monte-carlo-runs", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(2)));
    }

    @Test
    void autoGeneratesSeedWhenAbsent() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(post("/api/v1/scenarios/{id}/monte-carlo-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"NPV","iterations":1000,
                                 "variables":[{"variable":"UNIT_COST","type":"NORMAL","mean":0,"stdDev":0.1}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seed", notNullValue()));
    }

    @Test
    void rejectsInvalidDistribution() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(post("/api/v1/scenarios/{id}/monte-carlo-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"NPV","iterations":1000,
                                 "variables":[{"variable":"PRICE","type":"GAMMA"}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidVariable() throws Exception {
        Long scenarioId = createBaseScenario();
        mockMvc.perform(post("/api/v1/scenarios/{id}/monte-carlo-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"NPV","iterations":1000,
                                 "variables":[{"variable":"NOPE","type":"TRIANGULAR","min":-0.2,"mode":0,"max":0.2}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    private Long createBaseScenario() throws Exception {
        Long projectId = createProject("SIS-R11-MC");
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"蒙特卡洛基准\",\"horizonYears\":5,\"constructionYears\":1}"))
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

    private Long extractRunId(String response) {
        return Long.valueOf(response.replaceAll("(?s).*\\\"runId\\\":(\\d+).*", "$1"));
    }
}
