package com.sis.iids.report;

import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationResultRepository;
import com.sis.iids.calculation.CalculationStatus;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CalculationTaskRepository;
import com.sis.iids.calculation.CashFlowRow;
import com.sis.iids.calculation.CashFlowRowRepository;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CalculationTaskRepository calculationTaskRepository;
    private final CalculationResultRepository calculationResultRepository;
    private final CashFlowRowRepository cashFlowRowRepository;
    private final ReportDocumentRepository reportDocumentRepository;
    private final AuditService auditService;
    private final Path reportDir;

    public ReportService(CalculationTaskRepository calculationTaskRepository,
                         CalculationResultRepository calculationResultRepository,
                         CashFlowRowRepository cashFlowRowRepository,
                         ReportDocumentRepository reportDocumentRepository,
                         AuditService auditService,
                         @Value("${iids.report-dir:./data/reports}") String reportDir) {
        this.calculationTaskRepository = calculationTaskRepository;
        this.calculationResultRepository = calculationResultRepository;
        this.cashFlowRowRepository = cashFlowRowRepository;
        this.reportDocumentRepository = reportDocumentRepository;
        this.auditService = auditService;
        this.reportDir = Path.of(reportDir);
    }

    @Transactional
    public ReportDocumentResponse generate(Long taskId) {
        CalculationTask task = calculationTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Calculation task not found"));
        if (task.getStatus() != CalculationStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.CONFLICT, "Only successful calculation tasks can generate reports");
        }
        List<CalculationResultEntity> metrics = calculationResultRepository.findByTaskIdOrderByMetricCodeAsc(taskId);
        if (metrics.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Calculation results are required");
        }
        List<CashFlowRow> cashFlowRows = cashFlowRowRepository.findByTaskIdOrderByPeriodNoAsc(taskId);

        String fileName = "investment-report-task-%d-%s.xlsx".formatted(taskId, LocalDateTime.now().format(FILE_TIME));
        Path filePath = reportDir.resolve(fileName).toAbsolutePath().normalize();
        writeWorkbook(filePath, metrics, cashFlowRows);

        ReportDocument document = new ReportDocument();
        document.setScenarioId(task.getScenarioId());
        document.setTaskId(task.getId());
        document.setTitle("Investment Return Report - Task " + task.getId());
        document.setFilePath(filePath.toString());
        document.setFileType("EXCEL");
        document.setStatus(ReportDocumentStatus.GENERATED);
        ReportDocument saved = reportDocumentRepository.save(document);
        auditService.record("REPORT_GENERATED", "REPORT_DOCUMENT", saved.getId().toString(), null,
                "taskId=%s;scenarioId=%s;fileName=%s".formatted(task.getId(), task.getScenarioId(), fileName));
        return ReportDocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ReportDownload download(Long reportId) {
        ReportDocument document = reportDocumentRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Report document not found"));
        Path path = Path.of(document.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Report file not found");
        }
        try {
            byte[] content = Files.readAllBytes(path);
            auditService.record("REPORT_DOWNLOADED", "REPORT_DOCUMENT", document.getId().toString(), null, document.getFilePath());
            return new ReportDownload(path.getFileName().toString(), content);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Report file could not be read");
        }
    }

    private void writeWorkbook(Path filePath, List<CalculationResultEntity> metrics, List<CashFlowRow> cashFlowRows) {
        try {
            Files.createDirectories(filePath.getParent());
            try (Workbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(filePath)) {
                writeMetricSheet(workbook, metrics);
                writeCashFlowSheet(workbook, cashFlowRows);
                workbook.write(out);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Report file could not be generated");
        }
    }

    private void writeMetricSheet(Workbook workbook, List<CalculationResultEntity> metrics) {
        Sheet sheet = workbook.createSheet("Metric Summary");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Metric Code");
        header.createCell(1).setCellValue("Metric Value");
        header.createCell(2).setCellValue("Formula Version");
        header.createCell(3).setCellValue("Engine Version");
        header.createCell(4).setCellValue("Parameter Set Id");
        header.createCell(5).setCellValue("Input Hash");
        for (int i = 0; i < metrics.size(); i++) {
            CalculationResultEntity metric = metrics.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(metric.getMetricCode());
            numeric(row, 1, metric.getMetricValue());
            row.createCell(2).setCellValue(metric.getFormulaVersion());
            row.createCell(3).setCellValue(metric.getEngineVersion());
            if (metric.getParameterSetId() != null) {
                row.createCell(4).setCellValue(metric.getParameterSetId());
            }
            row.createCell(5).setCellValue(metric.getInputHash() == null ? "" : metric.getInputHash());
        }
    }

    private void writeCashFlowSheet(Workbook workbook, List<CashFlowRow> cashFlowRows) {
        Sheet sheet = workbook.createSheet("Cash Flow");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Statement Type");
        header.createCell(1).setCellValue("Period No");
        header.createCell(2).setCellValue("Inflow");
        header.createCell(3).setCellValue("Outflow");
        header.createCell(4).setCellValue("Net Cash Flow");
        header.createCell(5).setCellValue("Discounted Cash Flow");
        header.createCell(6).setCellValue("Cumulative Cash Flow");
        for (int i = 0; i < cashFlowRows.size(); i++) {
            CashFlowRow cashFlowRow = cashFlowRows.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(cashFlowRow.getStatementType());
            row.createCell(1).setCellValue(cashFlowRow.getPeriodNo());
            numeric(row, 2, cashFlowRow.getInflow());
            numeric(row, 3, cashFlowRow.getOutflow());
            numeric(row, 4, cashFlowRow.getNetCashFlow());
            numeric(row, 5, cashFlowRow.getDiscountedCashFlow());
            numeric(row, 6, cashFlowRow.getCumulativeCashFlow());
        }
    }

    private void numeric(Row row, int column, BigDecimal value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.doubleValue());
        }
    }
}