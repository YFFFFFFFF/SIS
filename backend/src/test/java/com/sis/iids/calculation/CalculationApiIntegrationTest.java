package com.sis.iids.calculation;

import com.sis.iids.worker.CalculationWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "iids.worker.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CalculationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalculationResultRepository resultRepository;

    @Autowired
    private CalculationWorker calculationWorker;

    @Test
    void createsPendingTaskAndWorkerPersistsSuccessfulResults() throws Exception {
        Long projectId = createProject("SIS-M1-CALC");
        Long scenarioId = createScenario(projectId);
        upsertParameters(scenarioId);
        createInvestmentItem(scenarioId, "CONSTRUCTION", "Construction Investment", 200000, 0);
        createInvestmentItem(scenarioId, "WORKING_CAPITAL", "Working Capital", 20000, 1);
        createFinancingPlan(scenarioId, "EQUITY", 1.0, 220000, 0, 0);

        String taskResponse = createCalculationTask(scenarioId, "calc-success-001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.id", notNullValue()))
                .andExpect(jsonPath("$.data.task.status").value("PENDING"))
                .andExpect(jsonPath("$.data.task.progress").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long taskId = extractNestedTaskId(taskResponse);

        calculationWorker.runPendingOnce();

        mockMvc.perform(get("/api/v1/calculation-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.progress").value(100))
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId));

        mockMvc.perform(get("/api/v1/calculation-tasks/{taskId}/results", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.TOTAL_INVESTMENT").value(220000.0000))
                .andExpect(jsonPath("$.data.metrics.NPV").value(86204.4011))
                .andExpect(jsonPath("$.data.metrics.ROI").value(0.1875))
                .andExpect(jsonPath("$.data.metrics.IRR").value(0.2391))
                .andExpect(jsonPath("$.data.metrics.CAPITAL_NET_PROFIT_RATE").value(0.1705))
                .andExpect(jsonPath("$.data.metrics.DYNAMIC_PAYBACK_YEARS").value(3.5152))
                .andExpect(jsonPath("$.data.cashFlowRows", hasSize(6)))
                .andExpect(jsonPath("$.data.cashFlowRows[1].netCashFlow").value(77500.0000));

        List<CalculationResultEntity> persistedResults = resultRepository.findByTaskIdOrderByMetricCodeAsc(taskId);
        assertThat(persistedResults).hasSize(7);
        assertThat(persistedResults).allSatisfy(result -> assertThat(result.getInputHash()).isNotBlank());
    }

    @Test
    void returnsExistingTaskForSameRequestKey() throws Exception {
        Long projectId = createProject("SIS-M1-IDEMPOTENT");
        Long scenarioId = createScenario(projectId);

        String firstResponse = createCalculationTask(scenarioId, "same-key")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String secondResponse = createCalculationTask(scenarioId, "same-key")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(extractNestedTaskId(secondResponse)).isEqualTo(extractNestedTaskId(firstResponse));
    }

    @Test
    void workerStoresFailureStatusAndErrorMessage() throws Exception {
        Long projectId = createProject("SIS-M1-FAIL");
        Long scenarioId = createScenario(projectId);

        String taskResponse = createCalculationTask(scenarioId, "calc-failure-001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long taskId = extractNestedTaskId(taskResponse);

        calculationWorker.runPendingOnce();

        mockMvc.perform(get("/api/v1/calculation-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.progress").value(100))
                .andExpect(jsonPath("$.data.errorMessage").value("Parameter set is required"));
    }

    private org.springframework.test.web.servlet.ResultActions createCalculationTask(Long scenarioId, String requestKey) throws Exception {
        String request = """
                {"taskType":"FINANCIAL","requestKey":"%s"}
                """.formatted(requestKey);
        return mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/calculation-tasks", scenarioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }

    private Long createProject(String code) throws Exception {
        String request = """
                {"code":"%s","name":"Calculation Host Project","projectType":"INDUSTRIAL"}
                """.formatted(code);
        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long createScenario(Long projectId) throws Exception {
        String request = """
                {"name":"Baseline Calculation Scenario","horizonYears":5,"constructionYears":1,"remarks":"calculation baseline"}
                """;
        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private void upsertParameters(Long scenarioId) throws Exception {
        String request = """
                {
                  "wacc": 0.10,
                  "waccSource": "manual benchmark",
                  "taxRate": 0.25,
                  "depreciationYears": 5,
                  "residualRate": 0,
                  "loanRatioLimit": 0.70,
                  "pricePerUnit": 140,
                  "unitCost": 40,
                  "annualOutput": 1000,
                  "fixedOperatingCost": 10000,
                  "formulaVersion": "fin-m1-1.0.0"
                }
                """;
        mockMvc.perform(put("/api/v1/scenarios/{scenarioId}/parameters", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    private void createInvestmentItem(Long scenarioId, String category, String name, int amount, int yearNo) throws Exception {
        String request = """
                {"category":"%s","name":"%s","amount":%d,"yearNo":%d}
                """.formatted(category, name, amount, yearNo);
        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    private void createFinancingPlan(Long scenarioId, String sourceType, double ratio, int amount,
                                     double interestRate, int termYears) throws Exception {
        String request = """
                {"sourceType":"%s","ratio":%s,"amount":%d,"interestRate":%s,"termYears":%d}
                """.formatted(sourceType, ratio, amount, interestRate, termYears);
        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/financing-plans", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    private Long extractId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }

    private Long extractNestedTaskId(String response) {
        return Long.valueOf(response.replaceAll(".*\\\"task\\\":\\{\\\"id\\\":(\\d+).*", "$1"));
    }
}
