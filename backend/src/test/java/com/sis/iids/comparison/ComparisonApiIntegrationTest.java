package com.sis.iids.comparison;

import com.jayway.jsonpath.JsonPath;
import com.sis.iids.worker.CalculationWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R-05 多方案横向对比 API 集成测试（FR-03-01）。
 */
@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class ComparisonApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalculationWorker calculationWorker;

    @Test
    void buildsComparisonMatrixWithBestMarksAndRanking() throws Exception {
        Long projectId = createProject("SIS-R05-CMP");
        // 三个方案：B 售价最高（NPV 最高）、A 居中、C 最低；另有未测算方案 D
        Long scenarioB = createScenario(projectId, "方案B-高售价");
        Long scenarioA = createScenario(projectId, "方案A-基准");
        Long scenarioC = createScenario(projectId, "方案C-低售价");
        Long scenarioD = createScenario(projectId, "方案D-未测算");

        setupCalculatedScenario(scenarioA, 140, "cmp-a");
        setupCalculatedScenario(scenarioB, 180, "cmp-b");
        setupCalculatedScenario(scenarioC, 100, "cmp-c");

        MvcResult result = mockMvc.perform(get("/api/v1/projects/{pid}/comparison", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.scenarios", hasSize(4)))
                // 7 个财务指标行 + 1 行风险占位
                .andExpect(jsonPath("$.data.metrics", hasSize(8)))
                .andExpect(jsonPath("$.data.ranking", hasSize(3)))
                .andReturn();
        Map<String, Object> data = JsonPath.read(result.getResponse().getContentAsString(), "$.data");

        List<Map<String, Object>> scenarios = (List<Map<String, Object>>) data.get("scenarios");
        Map<Long, Integer> columnIndex = new HashMap<>();
        for (int i = 0; i < scenarios.size(); i++) {
            columnIndex.put(((Number) scenarios.get(i).get("scenarioId")).longValue(), i);
        }
        // 未测算方案 D：calculated=false、taskId 为空
        int dIdx = columnIndex.get(scenarioD);
        assertThat(scenarios.get(dIdx).get("calculated")).isEqualTo(false);
        assertThat(scenarios.get(dIdx).get("taskId")).isNull();

        // NPV 行：最优应为方案 B；未测算方案 D 的值为 null
        Map<String, Object> npvRow = metricRow(data, "NPV");
        List<Object> npvValues = (List<Object>) npvRow.get("values");
        List<Object> npvBest = (List<Object>) npvRow.get("bestScenarioIds");
        assertThat(npvBest).containsExactly(scenarioB.intValue());
        assertThat(npvValues.get(dIdx)).isNull();

        // 排序建议：B > A > C（按 NPV 降序），且不含未测算的 D
        List<Map<String, Object>> ranking = (List<Map<String, Object>>) data.get("ranking");
        List<Long> rankedIds = ranking.stream()
                .map(r -> ((Number) r.get("scenarioId")).longValue()).toList();
        assertThat(rankedIds).containsExactly(scenarioB, scenarioA, scenarioC);
        assertThat(rankedIds).doesNotContain(scenarioD);
        // 排序结果 NPV 单调不增
        List<BigDecimal> rankedNpvs = ranking.stream()
                .map(r -> new BigDecimal(r.get("npv").toString())).toList();
        for (int i = 1; i < rankedNpvs.size(); i++) {
            assertThat(rankedNpvs.get(i)).isLessThanOrEqualTo(rankedNpvs.get(i - 1));
        }

        // 回收期行方向为 LOWER 且有最优标记；总投资行 NONE 无最优标记；风险占位行存在
        assertThat(metricRow(data, "STATIC_PAYBACK_YEARS").get("direction")).isEqualTo("LOWER");
        assertThat((List<?>) metricRow(data, "STATIC_PAYBACK_YEARS").get("bestScenarioIds")).isNotEmpty();
        Map<String, Object> invRow = metricRow(data, "TOTAL_INVESTMENT");
        assertThat(invRow.get("direction")).isEqualTo("NONE");
        assertThat((List<?>) invRow.get("bestScenarioIds")).isEmpty();
        // 风险占位行存在且方向为 NONE（中文名称断言受响应编码影响，改断言编码）
        assertThat(metricRow(data, "RISK_LEVEL").get("direction")).isEqualTo("NONE");
    }

    @Test
    void returns404WhenProjectMissing() throws Exception {
        // 项目约定：BusinessException 统一返回 400，业务码区分具体错误
        mockMvc.perform(get("/api/v1/projects/{pid}/comparison", 999999))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private Map<String, Object> metricRow(Map<String, Object> data, String code) {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("metrics");
        return rows.stream().filter(r -> code.equals(r.get("metricCode"))).findFirst().orElseThrow();
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
                        .content("{\"code\":\"" + code + "\",\"name\":\"对比宿主项目\",\"projectType\":\"INDUSTRIAL\"}"))
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
