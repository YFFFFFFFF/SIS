package com.sis.iids.perf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-18 非功能收口：性能对标测试（PRD 5.1）。
 * 测算 ≤ 3 分钟、蒙特卡洛（1 万次）≤ 1 分钟、看板聚合 ≤ 3 秒。
 * 以宽松阈值锁定量级（CI 机器抖动容忍），真实性能以实测记录为准（见 upgrade_plan §12）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class PerformanceBenchmarkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.sis.iids.calculation.CalculationService calculationService;

    @Test
    void calculationWithinBudget() throws Exception {
        Long scenarioId = createBaseScenario("SIS-R18-CALC");
        String taskResp = mockMvc.perform(post("/api/v1/scenarios/{id}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"taskType\":\"FULL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long taskId = Long.valueOf(taskResp.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1"));

        long start = System.currentTimeMillis();
        calculationService.executeTask(taskId);
        long elapsed = System.currentTimeMillis() - start;
        // PRD：测算 ≤ 3 分钟；实测毫秒级，锁定 30s 宽松阈值防回归
        assertThat(elapsed).isLessThan(30000);
        System.out.println("[PERF] 财务测算耗时: " + elapsed + " ms（预算 180000 ms）");
    }

    @Test
    void monteCarloWithinBudget() throws Exception {
        Long scenarioId = createBaseScenario("SIS-R18-MC");
        long start = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/scenarios/{id}/monte-carlo-runs", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetMetric":"NPV","iterations":10000,"seed":42,
                                 "variables":[{"variable":"PRICE","type":"TRIANGULAR","min":-0.2,"mode":0,"max":0.2},
                                              {"variable":"UNIT_COST","type":"NORMAL","mean":0,"stdDev":0.1}]}
                                """))
                .andExpect(status().isOk());
        long elapsed = System.currentTimeMillis() - start;
        // PRD：蒙特卡洛 ≤ 1 分钟
        assertThat(elapsed).isLessThan(60000);
        System.out.println("[PERF] 蒙特卡洛 1 万次耗时: " + elapsed + " ms（预算 60000 ms）");
    }

    @Test
    void dashboardWithinBudget() throws Exception {
        createBaseScenario("SIS-R18-DASH");
        // 预热
        mockMvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isOk());
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isOk());
        long elapsed = System.currentTimeMillis() - start;
        // PRD：看板 ≤ 3 秒
        assertThat(elapsed).isLessThan(3000);
        System.out.println("[PERF] 看板聚合耗时: " + elapsed + " ms（预算 3000 ms）");
    }

    private Long createBaseScenario(String code) throws Exception {
        Long projectId = createProject(code);
        String scenarioResp = mockMvc.perform(post("/api/v1/projects/{pid}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"性能基准\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long scenarioId = Long.valueOf(scenarioResp.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1"));
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
        return Long.valueOf(resp.replaceAll("(?s).*\\\"id\\\":(\\d+).*", "$1"));
    }
}
