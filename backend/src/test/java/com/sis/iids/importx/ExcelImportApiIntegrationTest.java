package com.sis.iids.importx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sis.iids.calculation.FinancingPlanRepository;
import com.sis.iids.calculation.InvestmentItemRepository;
import com.sis.iids.scenario.ParameterSetRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = {"ADMIN", "INVESTMENT_ANALYST", "FINANCE_SPECIALIST", "TECHNICAL_ENGINEER", "PROJECT_MANAGER", "SYSTEM_ADMINISTRATOR"})
@Transactional
class ExcelImportApiIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParameterSetRepository parameterSetRepository;

    @Autowired
    private InvestmentItemRepository investmentItemRepository;

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Test
    void uploadsValidTemplateAndImportsScenarioData() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-IMPORT-OK"));
        MockMultipartFile file = excelFile("m1-template.xlsx", validWorkbook());

        String response = mockMvc.perform(multipart("/api/v1/scenarios/{scenarioId}/import/excel", scenarioId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.data.fileName").value("m1-template.xlsx"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message", containsString("Excel 模板导入成功")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long jobId = extractId(response);

        assertThat(parameterSetRepository.findByScenarioId(scenarioId)).isPresent();
        assertThat(parameterSetRepository.findByScenarioId(scenarioId).orElseThrow().getWacc()).isEqualByComparingTo("0.10");
        assertThat(investmentItemRepository.findByScenarioId(scenarioId)).hasSize(2);
        assertThat(financingPlanRepository.findByScenarioId(scenarioId)).hasSize(1);
        assertThat(importJobRepository.findById(jobId).orElseThrow().getStatus()).isEqualTo(ImportJobStatus.SUCCESS);

        mockMvc.perform(get("/api/v1/import-jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void invalidTemplateCreatesFailedJobWithFieldLevelMessage() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-IMPORT-FAIL"));
        MockMultipartFile file = excelFile("bad-template.xlsx", invalidWorkbook());

        String response = mockMvc.perform(multipart("/api/v1/scenarios/{scenarioId}/import/excel", scenarioId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.message", containsString("InvestmentItems 第 2 行字段 amount 必须是数字")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long jobId = extractId(response);

        assertThat(importJobRepository.findById(jobId).orElseThrow().getStatus()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(investmentItemRepository.findByScenarioId(scenarioId)).isEmpty();
        assertThat(financingPlanRepository.findByScenarioId(scenarioId)).isEmpty();
    }

    @Test
    void successfulRetryReplacesRowsInsteadOfDuplicatingThem() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-IMPORT-RETRY"));
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(multipart("/api/v1/scenarios/{scenarioId}/import/excel", scenarioId)
                            .file(excelFile("retry.xlsx", validWorkbook())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.message", containsString("投资项 2 条")));
        }
        assertThat(investmentItemRepository.findByScenarioId(scenarioId)).hasSize(2);
        assertThat(financingPlanRepository.findByScenarioId(scenarioId)).hasSize(1);
    }

    @Test
    void emptyFileCreatesFailedJobWithoutWritingData() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-IMPORT-EMPTY"));

        mockMvc.perform(multipart("/api/v1/scenarios/{scenarioId}/import/excel", scenarioId)
                        .file(excelFile("empty.xlsx", new byte[0])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.message").value("请上传 Excel 文件"));

        assertThat(parameterSetRepository.findByScenarioId(scenarioId)).isEmpty();
        assertThat(investmentItemRepository.findByScenarioId(scenarioId)).isEmpty();
        assertThat(financingPlanRepository.findByScenarioId(scenarioId)).isEmpty();
    }

    @Test
    void failedRetryKeepsPreviouslyImportedDataIntact() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-IMPORT-ATOMIC"));
        mockMvc.perform(multipart("/api/v1/scenarios/{scenarioId}/import/excel", scenarioId)
                        .file(excelFile("valid.xlsx", validWorkbook())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(multipart("/api/v1/scenarios/{scenarioId}/import/excel", scenarioId)
                        .file(excelFile("invalid-retry.xlsx", invalidWorkbook("0.20"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        assertThat(parameterSetRepository.findByScenarioId(scenarioId).orElseThrow().getWacc()).isEqualByComparingTo("0.10");
        assertThat(investmentItemRepository.findByScenarioId(scenarioId)).hasSize(2);
        assertThat(financingPlanRepository.findByScenarioId(scenarioId)).hasSize(1);
    }

    @Test
    void importsHighRowCountWorkbook() throws Exception {
        Long scenarioId = createScenario(createProject("SIS-M1-IMPORT-LARGE"));

        mockMvc.perform(multipart("/api/v1/scenarios/{scenarioId}/import/excel", scenarioId)
                        .file(excelFile("large.xlsx", highRowCountWorkbook(1000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message", containsString("投资项 1000 条")));

        assertThat(investmentItemRepository.findByScenarioId(scenarioId)).hasSize(1000);
        assertThat(financingPlanRepository.findByScenarioId(scenarioId)).hasSize(1);
    }

    private MockMultipartFile excelFile(String fileName, byte[] content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content);
    }

    private byte[] validWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            parameterSheet(workbook);
            investmentSheet(workbook, "200000");
            financingSheet(workbook);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] invalidWorkbook() throws Exception {
        return invalidWorkbook("0.10");
    }

    private byte[] invalidWorkbook(String wacc) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            parameterSheet(workbook, wacc);
            investmentSheet(workbook, "not-a-number");
            financingSheet(workbook);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] highRowCountWorkbook(int itemCount) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            parameterSheet(workbook);
            Sheet sheet = workbook.createSheet("InvestmentItems");
            row(sheet, 0, "category", "name", "amount", "yearNo");
            for (int index = 1; index <= itemCount; index++) {
                row(sheet, index, "CONSTRUCTION", "Item " + index, "100", "0");
            }
            financingSheet(workbook);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void parameterSheet(XSSFWorkbook workbook) {
        parameterSheet(workbook, "0.10");
    }

    private void parameterSheet(XSSFWorkbook workbook, String wacc) {
        Sheet sheet = workbook.createSheet("Parameters");
        row(sheet, 0, "field", "value");
        row(sheet, 1, "wacc", wacc);
        row(sheet, 2, "taxRate", "0.25");
        row(sheet, 3, "depreciationYears", "5");
        row(sheet, 4, "residualRate", "0");
        row(sheet, 5, "loanRatioLimit", "0.70");
        row(sheet, 6, "pricePerUnit", "140");
        row(sheet, 7, "unitCost", "40");
        row(sheet, 8, "annualOutput", "1000");
        row(sheet, 9, "fixedOperatingCost", "10000");
        row(sheet, 10, "formulaVersion", "fin-m1-1.0.0");
    }

    private void investmentSheet(XSSFWorkbook workbook, String constructionAmount) {
        Sheet sheet = workbook.createSheet("InvestmentItems");
        row(sheet, 0, "category", "name", "amount", "yearNo");
        row(sheet, 1, "CONSTRUCTION", "Construction Investment", constructionAmount, "0");
        row(sheet, 2, "WORKING_CAPITAL", "Working Capital", "20000", "1");
    }

    private void financingSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("FinancingPlans");
        row(sheet, 0, "sourceType", "ratio", "amount", "interestRate", "termYears");
        row(sheet, 1, "EQUITY", "1.0", "220000", "0", "0");
    }

    private void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private Long createProject(String code) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"name\":\"Import Host Project\",\"projectType\":\"INDUSTRIAL\"}".formatted(code)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long createScenario(Long projectId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/scenarios", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Import Scenario\",\"horizonYears\":5,\"constructionYears\":1}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long extractId(String response) throws Exception {
        return OBJECT_MAPPER.readTree(response).path("data").path("id").asLong();
    }
}
