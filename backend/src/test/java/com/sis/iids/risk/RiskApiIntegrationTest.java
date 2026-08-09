package com.sis.iids.risk;

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
 * R-12 智能风险预警 API 集成测试（FR-02-04）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class RiskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.sis.iids.calculation.CalculationService calculationService;

    @Test
    void seededRulesAreListed() throws Exception {
        mockMvc.perform(get("/api/v1/risk-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(3)));
    }

    @Test
    void ruleCrudLifecycle() throws Exception {
        String resp = mockMvc.perform(post("/api/v1/risk-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metricCode":"STATIC_PAYBACK_YEARS","direction":"ABOVE","thresholdValue":6,
                                 "level":"YELLOW","strategy":"回收期超过 6 年：建议压缩建设投资或提升投产速度。"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        Long ruleId = extractId(resp);

        mockMvc.perform(put("/api/v1/risk-rules/{id}", ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metricCode":"STATIC_PAYBACK_YEARS","direction":"ABOVE","thresholdValue":5,
                                 "level":"RED","strategy":"收紧阈值","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("RED"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(delete("/api/v1/risk-rules/{id}", ruleId))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidRule() throws Exception {
        mockMvc.perform(post("/api/v1/risk-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metricCode":"NOPE","direction":"ABOVE","thresholdValue":1,"level":"RED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluatesAndAcknowledgesAlerts() throws Exception {
        // 低收益方案：price=60 → NPV 为负、IRR 低 → 触发种子规则
        Long scenarioId = createScenario("SIS-R12-LOW", 60);

        String evalResp = mockMvc.perform(post("/api/v1/scenarios/{id}/risk-alerts/evaluate", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.triggered.length()", greaterThanOrEqualTo(1)))
                .andReturn().getResponse().getContentAsString();
        Long alertId = extractFirstAlertId(evalResp);

        mockMvc.perform(get("/api/v1/risk-alerts").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/v1/risk-alerts/{id}/ack", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKED"))
                .andExpect(jsonPath("$.data.ackBy", notNullValue()));

        // 重复确认被拒
        mockMvc.perform(post("/api/v1/risk-alerts/{id}/ack", alertId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recoversWhenMetricBackToNormal() throws Exception {
        Long scenarioId = createScenario("SIS-R12-REC", 60);
        mockMvc.perform(post("/api/v1/scenarios/{id}/risk-alerts/evaluate", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.triggered.length()", greaterThanOrEqualTo(1)));

        // 提升售价后重新测算并评估 → 原 OPEN 事件应标记 RECOVERED
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                                 "pricePerUnit":140,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000}
                                """))
                .andExpect(status().isOk());
        executeCalculation(scenarioId);

        mockMvc.perform(post("/api/v1/scenarios/{id}/risk-alerts/evaluate", scenarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recovered.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/scenarios/{id}/risk-alerts", scenarioId))
                .andExpect(status().isOk());
    }

    @Test
    void evaluateWithoutCalculationReturns400() throws Exception {
        Long scenarioId = createScenarioWithoutCalculation("SIS-R12-NOCALC");
        mockMvc.perform(post("/api/v1/scenarios/{id}/risk-alerts/evaluate", scenarioId))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // fixtures
    // ============================================================
    private Long createScenario(String code, int price) throws Exception {
        Long scenarioId = createScenarioWithoutCalculation(code);
        mockMvc.perform(put("/api/v1/scenarios/{id}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                                {"wacc":0.10,"taxRate":0.25,"depreciationYears":5,"residualRate":0,"loanRatioLimit":0.70,
                                 "pricePerUnit":%d,"unitCost":40,"annualOutput":1000,"fixedOperatingCost":10000}
                                """).formatted(price)))
                .andExpect(status().isOk());
        executeCalculation(scenarioId);
        return scenarioId;
    }

    /** 测试环境 worker 关闭（iids.worker.enabled=false），直接同步执行任务。 */
    private void executeCalculation(Long scenarioId) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/scenarios/{id}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"taskType\":\"FULL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long taskId = extractId(resp);
        calculationService.executeTask(taskId);
    }

    private Long createScenarioWithoutCalculation(String code) throws Exception {
        Long projectId = createProject(code);
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"风险预警场景\",\"horizonYears\":5,\"constructionYears\":1}"))
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

    private Long extractFirstAlertId(String response) {
        String triggered = response.replaceAll("(?s).*\\\"triggered\\\":\\[", "");
        return Long.valueOf(triggered.replaceAll("(?s)^\\{\\\"id\\\":(\\d+).*", "$1"));
    }
}
