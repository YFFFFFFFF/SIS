package com.sis.iids.importx;

import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.FinancingPlan;
import com.sis.iids.calculation.FinancingPlanRepository;
import com.sis.iids.calculation.InvestmentItem;
import com.sis.iids.calculation.InvestmentItemRepository;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.scenario.ParameterSet;
import com.sis.iids.scenario.ParameterSetRepository;
import com.sis.iids.scenario.ScenarioRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelImportService {

    private final ScenarioRepository scenarioRepository;
    private final ParameterSetRepository parameterSetRepository;
    private final InvestmentItemRepository investmentItemRepository;
    private final FinancingPlanRepository financingPlanRepository;
    private final ImportJobRepository importJobRepository;
    private final AuditService auditService;
    private final DataFormatter dataFormatter = new DataFormatter();

    public ExcelImportService(ScenarioRepository scenarioRepository,
                              ParameterSetRepository parameterSetRepository,
                              InvestmentItemRepository investmentItemRepository,
                              FinancingPlanRepository financingPlanRepository,
                              ImportJobRepository importJobRepository,
                              AuditService auditService) {
        this.scenarioRepository = scenarioRepository;
        this.parameterSetRepository = parameterSetRepository;
        this.investmentItemRepository = investmentItemRepository;
        this.financingPlanRepository = financingPlanRepository;
        this.importJobRepository = importJobRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ImportJobResponse importExcel(Long scenarioId, MultipartFile file) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在");
        }
        String fileName = normalizeFileName(file.getOriginalFilename());
        try {
            ParsedImport parsed = parse(file, scenarioId);
            parameterSetRepository.save(parsed.parameterSet());
            investmentItemRepository.saveAll(parsed.investmentItems());
            financingPlanRepository.saveAll(parsed.financingPlans());
            ImportJob job = saveJob(scenarioId, fileName, ImportJobStatus.SUCCESS, "Excel 模板导入成功");
            auditService.record("IMPORT_SUCCESS", "IMPORT_JOB", job.getId().toString(), null,
                    "scenarioId=%s;fileName=%s".formatted(scenarioId, fileName));
            return ImportJobResponse.from(job);
        } catch (ImportValidationException ex) {
            ImportJob job = saveJob(scenarioId, fileName, ImportJobStatus.FAILED, ex.getMessage());
            auditService.record("IMPORT_FAILURE", "IMPORT_JOB", job.getId().toString(), null, ex.getMessage());
            return ImportJobResponse.from(job);
        } catch (IOException ex) {
            ImportJob job = saveJob(scenarioId, fileName, ImportJobStatus.FAILED, "Excel 文件读取失败");
            auditService.record("IMPORT_FAILURE", "IMPORT_JOB", job.getId().toString(), null, job.getMessage());
            return ImportJobResponse.from(job);
        }
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getJob(Long jobId) {
        return ImportJobResponse.from(importJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "导入任务不存在")));
    }

    private ParsedImport parse(MultipartFile file, Long scenarioId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ImportValidationException("请上传 Excel 文件");
        }
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            ParameterSet parameterSet = parseParameters(requiredSheet(workbook, "Parameters"), scenarioId);
            List<InvestmentItem> investmentItems = parseInvestmentItems(requiredSheet(workbook, "InvestmentItems"), scenarioId);
            List<FinancingPlan> financingPlans = parseFinancingPlans(requiredSheet(workbook, "FinancingPlans"), scenarioId);
            return new ParsedImport(parameterSet, investmentItems, financingPlans);
        }
    }

    private ParameterSet parseParameters(Sheet sheet, Long scenarioId) {
        Map<String, String> values = new HashMap<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String field = cell(row, 0);
            if (field.isBlank()) {
                continue;
            }
            values.put(field, cell(row, 1));
        }

        ParameterSet parameterSet = parameterSetRepository.findByScenarioId(scenarioId).orElseGet(ParameterSet::new);
        parameterSet.setScenarioId(scenarioId);
        parameterSet.setWacc(decimal(values, "wacc"));
        parameterSet.setTaxRate(decimal(values, "taxRate"));
        parameterSet.setDepreciationYears(integer(values, "depreciationYears"));
        parameterSet.setResidualRate(decimal(values, "residualRate"));
        parameterSet.setLoanRatioLimit(decimal(values, "loanRatioLimit"));
        parameterSet.setPricePerUnit(decimal(values, "pricePerUnit"));
        parameterSet.setUnitCost(decimal(values, "unitCost"));
        parameterSet.setAnnualOutput(decimal(values, "annualOutput"));
        parameterSet.setFixedOperatingCost(decimal(values, "fixedOperatingCost"));
        parameterSet.setFormulaVersion(blankToNull(values.get("formulaVersion")));
        return parameterSet;
    }

    private List<InvestmentItem> parseInvestmentItems(Sheet sheet, Long scenarioId) {
        List<InvestmentItem> items = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlankRow(row, 4)) {
                continue;
            }
            int rowNumber = i + 1;
            InvestmentItem item = new InvestmentItem();
            item.setScenarioId(scenarioId);
            item.setCategory(requiredText(row, 0, "InvestmentItems", rowNumber, "category"));
            item.setName(requiredText(row, 1, "InvestmentItems", rowNumber, "name"));
            item.setAmount(decimal(row, 2, "InvestmentItems", rowNumber, "amount"));
            item.setYearNo(integer(row, 3, "InvestmentItems", rowNumber, "yearNo"));
            items.add(item);
        }
        return items;
    }

    private List<FinancingPlan> parseFinancingPlans(Sheet sheet, Long scenarioId) {
        List<FinancingPlan> plans = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlankRow(row, 5)) {
                continue;
            }
            int rowNumber = i + 1;
            FinancingPlan plan = new FinancingPlan();
            plan.setScenarioId(scenarioId);
            plan.setSourceType(requiredText(row, 0, "FinancingPlans", rowNumber, "sourceType"));
            plan.setRatio(decimal(row, 1, "FinancingPlans", rowNumber, "ratio"));
            plan.setAmount(decimal(row, 2, "FinancingPlans", rowNumber, "amount"));
            plan.setInterestRate(decimal(row, 3, "FinancingPlans", rowNumber, "interestRate"));
            plan.setTermYears(integer(row, 4, "FinancingPlans", rowNumber, "termYears"));
            plans.add(plan);
        }
        return plans;
    }

    private Sheet requiredSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new ImportValidationException("缺少必填工作表：%s".formatted(name));
        }
        return sheet;
    }

    private String requiredText(Row row, int column, String sheet, int rowNumber, String field) {
        String value = cell(row, column);
        if (value.isBlank()) {
            throw new ImportValidationException("%s 第 %d 行字段 %s 不能为空".formatted(sheet, rowNumber, field));
        }
        return value;
    }

    private BigDecimal decimal(Map<String, String> values, String field) {
        String value = values.get(field);
        if (value == null || value.isBlank()) {
            throw new ImportValidationException("Parameters 工作表字段 %s 不能为空".formatted(field));
        }
        return parseDecimal(value, "Parameters 工作表字段 %s".formatted(field));
    }

    private Integer integer(Map<String, String> values, String field) {
        String value = values.get(field);
        if (value == null || value.isBlank()) {
            throw new ImportValidationException("Parameters 工作表字段 %s 不能为空".formatted(field));
        }
        return parseInteger(value, "Parameters 工作表字段 %s".formatted(field));
    }

    private BigDecimal decimal(Row row, int column, String sheet, int rowNumber, String field) {
        String value = cell(row, column);
        if (value.isBlank()) {
            throw new ImportValidationException("%s 第 %d 行字段 %s 不能为空".formatted(sheet, rowNumber, field));
        }
        return parseDecimal(value, "%s 第 %d 行字段 %s".formatted(sheet, rowNumber, field));
    }

    private Integer integer(Row row, int column, String sheet, int rowNumber, String field) {
        String value = cell(row, column);
        if (value.isBlank()) {
            throw new ImportValidationException("%s 第 %d 行字段 %s 不能为空".formatted(sheet, rowNumber, field));
        }
        return parseInteger(value, "%s 第 %d 行字段 %s".formatted(sheet, rowNumber, field));
    }

    private BigDecimal parseDecimal(String value, String label) {
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new ImportValidationException("%s 必须是数字".formatted(label));
        }
    }

    private Integer parseInteger(String value, String label) {
        try {
            return new BigDecimal(value.trim().replace(",", "")).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new ImportValidationException("%s 必须是整数".formatted(label));
        }
    }

    private boolean isBlankRow(Row row, int columns) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < columns; i++) {
            if (!cell(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cell(Row row, int column) {
        if (row == null || row.getCell(column) == null) {
            return "";
        }
        return dataFormatter.formatCellValue(row.getCell(column)).trim();
    }

    private ImportJob saveJob(Long scenarioId, String fileName, ImportJobStatus status, String message) {
        ImportJob job = new ImportJob();
        job.setScenarioId(scenarioId);
        job.setFileName(fileName);
        job.setStatus(status);
        job.setMessage(message);
        job.setFinishedAt(LocalDateTime.now());
        return importJobRepository.save(job);
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "uploaded-template.xlsx";
        }
        return fileName.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ParsedImport(ParameterSet parameterSet,
                                List<InvestmentItem> investmentItems,
                                List<FinancingPlan> financingPlans) {
    }
}
