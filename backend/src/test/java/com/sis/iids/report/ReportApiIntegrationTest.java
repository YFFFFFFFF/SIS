package com.sis.iids.report;

import com.sis.iids.worker.CalculationWorker;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "iids.worker.enabled=false",
        "iids.report-dir=target/test-reports"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ReportApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalculationWorker calculationWorker;

    @Autowired
    private ReportDocumentRepository reportDocumentRepository;

    @Test
    void completedCalculationCanGenerateReportDocument() throws Exception {
        Long taskId = completedCalculationTask("SIS-M1-REPORT-GEN", "report-generation-001");

        String response = mockMvc.perform(post("/api/v1/calculation-tasks/{taskId}/reports", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.fileType").value("EXCEL"))
                .andExpect(jsonPath("$.data.status").value("GENERATED"))
                .andExpect(jsonPath("$.data.fileName", endsWith(".xlsx")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long reportId = extractId(response);

        ReportDocument document = reportDocumentRepository.findById(reportId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(ReportDocumentStatus.GENERATED);
        assertThat(document.getFilePath()).isNotBlank();
    }

    @Test
    void reportDownloadReturnsNonEmptyExcelFile() throws Exception {
        Long taskId = completedCalculationTask("SIS-M1-REPORT-DL", "report-download-001");
        String response = mockMvc.perform(post("/api/v1/calculation-tasks/{taskId}/reports", taskId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long reportId = extractId(response);

        MockHttpServletResponse download = mockMvc.perform(get("/api/v1/reports/{reportId}/download", reportId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn()
                .getResponse();

        byte[] content = download.getContentAsByteArray();
        assertThat(content.length).isGreaterThan(1000);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            assertThat(workbook.getSheet("Metric Summary")).isNotNull();
            assertThat(workbook.getSheet("Cash Flow")).isNotNull();
            assertThat(workbook.getSheet("Metric Summary").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("CAPITAL_NET_PROFIT_RATE");
        }
    }

    private Long completedCalculationTask(String projectCode, String requestKey) throws Exception {
        Long projectId = createProject(projectCode);
        Long scenarioId = createScenario(projectId);
        upsertParameters(scenarioId);
        createInvestmentItem(scenarioId, "CONSTRUCTION", "Construction Investment", 200000, 0);
        createInvestmentItem(scenarioId, "WORKING_CAPITAL", "Working Capital", 20000, 1);
        createFinancingPlan(scenarioId, "EQUITY", 1.0, 220000, 0, 0);

        String taskResponse = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/calculation-tasks", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskType\":\"FINANCIAL\",\"requestKey\":\"%s\"}".formatted(requestKey)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long taskId = extractNestedTaskId(taskResponse);
        calculationWorker.runPendingOnce();
        return taskId;
    }

    private Long createProject(String code) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"name\":\"Report Host Project\",\"projectType\":\"INDUSTRIAL\"}".formatted(code)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long createScenario(Long projectId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Report Scenario\",\"horizonYears\":5,\"constructionYears\":1}"))
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
        String request = "{\"category\":\"%s\",\"name\":\"%s\",\"amount\":%d,\"yearNo\":%d}"
                .formatted(category, name, amount, yearNo);
        mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/investment-items", scenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    private void createFinancingPlan(Long scenarioId, String sourceType, double ratio, int amount,
                                     double interestRate, int termYears) throws Exception {
        String request = "{\"sourceType\":\"%s\",\"ratio\":%s,\"amount\":%d,\"interestRate\":%s,\"termYears\":%d}"
                .formatted(sourceType, ratio, amount, interestRate, termYears);
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