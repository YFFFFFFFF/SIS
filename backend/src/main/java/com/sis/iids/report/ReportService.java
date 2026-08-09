package com.sis.iids.report;

import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationResultRepository;
import com.sis.iids.calculation.CalculationService;
import com.sis.iids.calculation.CalculationStatus;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CalculationTaskRepository;
import com.sis.iids.calculation.CashFlowRow;
import com.sis.iids.calculation.CashFlowRowRepository;
import com.sis.iids.calculation.InvestmentSummary;
import com.sis.iids.calculation.LoanScheduleResponse;
import com.sis.iids.calculation.ProfitFlowResponse;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.MetricCodes;
import com.sis.iids.project.Project;
import com.sis.iids.project.ProjectRepository;
import com.sis.iids.scenario.ParameterSet;
import com.sis.iids.scenario.ParameterSetRepository;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.scenario.ScenarioRepository;
import com.sis.iids.sensitivity.SensitivityRun;
import com.sis.iids.sensitivity.SensitivityRunRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * R-06 报告升级（FR-01-06）：结构化投资回报分析报告。
 * Excel 版含报告说明/项目概况/指标摘要/投资估算/现金流量表/利润流向/还本付息七个 sheet；
 * PDF 版为摘要报告（OpenPDF 内置字体，正文使用英文标签）。两版均在"报告说明"中
 * 引用参数集版本、公式版本、引擎版本与输入哈希，满足"引用参数版本与数据来源"约束。
 */
@Service
public class ReportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CalculationTaskRepository calculationTaskRepository;
    private final CalculationResultRepository calculationResultRepository;
    private final CashFlowRowRepository cashFlowRowRepository;
    private final ReportDocumentRepository reportDocumentRepository;
    private final CalculationService calculationService;
    private final ScenarioRepository scenarioRepository;
    private final ProjectRepository projectRepository;
    private final ParameterSetRepository parameterSetRepository;
    private final SensitivityRunRepository sensitivityRunRepository;
    private final AuditService auditService;
    private final Path reportDir;

    public ReportService(CalculationTaskRepository calculationTaskRepository,
                         CalculationResultRepository calculationResultRepository,
                         CashFlowRowRepository cashFlowRowRepository,
                         ReportDocumentRepository reportDocumentRepository,
                         CalculationService calculationService,
                         ScenarioRepository scenarioRepository,
                         ProjectRepository projectRepository,
                         ParameterSetRepository parameterSetRepository,
                         SensitivityRunRepository sensitivityRunRepository,
                         AuditService auditService,
                         @Value("${iids.report-dir:./data/reports}") String reportDir) {
        this.calculationTaskRepository = calculationTaskRepository;
        this.calculationResultRepository = calculationResultRepository;
        this.cashFlowRowRepository = cashFlowRowRepository;
        this.reportDocumentRepository = reportDocumentRepository;
        this.calculationService = calculationService;
        this.scenarioRepository = scenarioRepository;
        this.projectRepository = projectRepository;
        this.parameterSetRepository = parameterSetRepository;
        this.sensitivityRunRepository = sensitivityRunRepository;
        this.auditService = auditService;
        this.reportDir = Path.of(reportDir);
    }

    @Transactional
    public ReportDocumentResponse generate(Long taskId, String format) {
        ReportFormat reportFormat = ReportFormat.from(format);
        CalculationTask task = calculationTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测算任务不存在"));
        if (task.getStatus() != CalculationStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有测算成功的任务才能生成报表");
        }
        List<CalculationResultEntity> metrics = calculationResultRepository.findByTaskIdOrderByMetricCodeAsc(taskId);
        if (metrics.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "缺少测算结果，无法生成报表");
        }
        ReportContent content = buildContent(task, metrics);

        String fileName = "investment-report-task-%d-%s.%s".formatted(taskId,
                LocalDateTime.now().format(FILE_TIME), reportFormat.extension());
        Path filePath = reportDir.resolve(fileName).toAbsolutePath().normalize();
        if (reportFormat == ReportFormat.PDF) {
            new ReportPdfWriter().write(filePath, content);
        } else {
            writeWorkbook(filePath, content);
        }

        ReportDocument document = new ReportDocument();
        document.setScenarioId(task.getScenarioId());
        document.setTaskId(task.getId());
        document.setTitle("投资回报分析报告 - 任务 " + task.getId());
        document.setFilePath(filePath.toString());
        document.setFileType(reportFormat.name());
        document.setStatus(ReportDocumentStatus.GENERATED);
        ReportDocument saved = reportDocumentRepository.save(document);
        auditService.record("REPORT_GENERATED", "REPORT_DOCUMENT", saved.getId().toString(), null,
                "taskId=%s;scenarioId=%s;format=%s;fileName=%s".formatted(task.getId(), task.getScenarioId(),
                        reportFormat.name(), fileName));
        return ReportDocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ReportDownload download(Long reportId) {
        ReportDocument document = reportDocumentRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报表记录不存在"));
        Path path = Path.of(document.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表文件不存在");
        }
        try {
            byte[] content = Files.readAllBytes(path);
            auditService.record("REPORT_DOWNLOADED", "REPORT_DOCUMENT", document.getId().toString(), null, document.getFilePath());
            return new ReportDownload(path.getFileName().toString(), content, document.getFileType());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "报表文件读取失败");
        }
    }

    /** 汇总报告所需的全部结构化数据（图表数据由前端依据相同接口渲染，报告内嵌关键表）。 */
    private ReportContent buildContent(CalculationTask task, List<CalculationResultEntity> metrics) {
        Scenario scenario = scenarioRepository.findById(task.getScenarioId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在"));
        Project project = projectRepository.findById(scenario.getProjectId()).orElse(null);
        ParameterSet params = parameterSetRepository.findByScenarioId(scenario.getId()).orElse(null);
        List<CashFlowRow> cashFlowRows = cashFlowRowRepository.findByTaskIdOrderByPeriodNoAsc(task.getId());
        InvestmentSummary investment = calculationService.getInvestmentSummary(scenario.getId());
        List<ProfitFlowResponse> profitFlow = calculationService.getProfitFlow(task.getId());
        List<LoanScheduleResponse> loanSchedule = calculationService.getLoanSchedule(task.getId());
        List<SensitivityRun> sensitivityRuns = sensitivityRunRepository
                .findByScenarioIdOrderByCreatedAtDesc(scenario.getId());
        return new ReportContent(project, scenario, params, task, metrics, investment,
                cashFlowRows, profitFlow, loanSchedule,
                sensitivityRuns.isEmpty() ? null : sensitivityRuns.get(0));
    }

    // ---------- Excel ----------

    private void writeWorkbook(Path filePath, ReportContent c) {
        try {
            Files.createDirectories(filePath.getParent());
            try (Workbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(filePath)) {
                writeMetaSheet(workbook, c);
                writeOverviewSheet(workbook, c);
                writeMetricSheet(workbook, c.metrics());
                writeInvestmentSheet(workbook, c.investment());
                writeCashFlowSheet(workbook, c.cashFlowRows());
                writeProfitFlowSheet(workbook, c.profitFlow());
                writeLoanScheduleSheet(workbook, c.loanSchedule());
                workbook.write(out);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "报表文件生成失败");
        }
    }

    /** 报告说明：参数版本与数据来源引用（FR-01-06 约束）。 */
    private void writeMetaSheet(Workbook workbook, ReportContent c) {
        Sheet sheet = workbook.createSheet("报告说明");
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("报告生成时间", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        rows.put("测算任务ID", String.valueOf(c.task().getId()));
        if (c.params() != null) {
            rows.put("参数集ID", String.valueOf(c.params().getId()));
            rows.put("参数集创建时间", c.params().getCreatedAt() == null ? ""
                    : c.params().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            rows.put("WACC 来源", c.params().getWaccSource() == null ? "" : c.params().getWaccSource());
        }
        CalculationResultEntity first = c.metrics().get(0);
        rows.put("公式版本", first.getFormulaVersion());
        rows.put("引擎版本", first.getEngineVersion());
        rows.put("输入哈希", first.getInputHash() == null ? "" : first.getInputHash());
        if (c.latestSensitivity() != null) {
            SensitivityRun run = c.latestSensitivity();
            rows.put("敏感性分析结论", "最近一次运行 #%d：目标指标 %s，变量 %s/%s，基准值 %s".formatted(
                    run.getId(), run.getTargetMetric(), run.getVariable1(),
                    run.getVariable2() == null ? "-" : run.getVariable2(),
                    run.getBaseValue() == null ? "-" : run.getBaseValue().toPlainString()));
        } else {
            rows.put("敏感性分析结论", "尚未运行敏感性分析");
        }
        rows.put("投资建议", buildAdvice(c.metrics()));
        int r = 0;
        for (Map.Entry<String, String> e : rows.entrySet()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
    }

    private void writeOverviewSheet(Workbook workbook, ReportContent c) {
        Sheet sheet = workbook.createSheet("项目概况");
        Map<String, String> rows = new LinkedHashMap<>();
        if (c.project() != null) {
            rows.put("项目编码", c.project().getCode());
            rows.put("项目名称", c.project().getName());
            rows.put("项目类型", c.project().getProjectType() == null ? "" : c.project().getProjectType());
            rows.put("所属部门", c.project().getDepartment() == null ? "" : c.project().getDepartment());
        }
        rows.put("方案名称", c.scenario().getName());
        rows.put("方案版本", "v" + c.scenario().getVersionNo());
        rows.put("测算期（年）", String.valueOf(c.scenario().getHorizonYears()));
        rows.put("建设期（年）", String.valueOf(c.scenario().getConstructionYears()));
        rows.put("方案状态", c.scenario().getStatus().name());
        int r = 0;
        for (Map.Entry<String, String> e : rows.entrySet()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
    }

    private void writeMetricSheet(Workbook workbook, List<CalculationResultEntity> metrics) {
        Sheet sheet = workbook.createSheet("指标汇总");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("指标编码");
        header.createCell(1).setCellValue("指标值");
        header.createCell(2).setCellValue("公式版本");
        header.createCell(3).setCellValue("引擎版本");
        header.createCell(4).setCellValue("参数集ID");
        header.createCell(5).setCellValue("输入哈希");
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

    private void writeInvestmentSheet(Workbook workbook, InvestmentSummary inv) {
        Sheet sheet = workbook.createSheet("投资估算");
        int r = 0;
        Row h1 = sheet.createRow(r++);
        h1.createCell(0).setCellValue("汇总项");
        h1.createCell(1).setCellValue("金额（万元）");
        r = summaryRow(sheet, r, "建设投资合计", inv.constructionTotal());
        r = summaryRow(sheet, r, "建设期利息", inv.interestDuringConstruction());
        r = summaryRow(sheet, r, "流动资金", inv.workingCapital());
        r = summaryRow(sheet, r, "总投资", inv.totalInvestment());
        if (inv.declaredTotalInvestment() != null) {
            r = summaryRow(sheet, r, "申报总投资", inv.declaredTotalInvestment());
        }
        Row bal = sheet.createRow(r++);
        bal.createCell(0).setCellValue("口径校验");
        bal.createCell(1).setCellValue(inv.balanced() ? "平衡" : "不平衡");
        r++;
        Row h2 = sheet.createRow(r++);
        h2.createCell(0).setCellValue("分项类别");
        h2.createCell(1).setCellValue("分项名称");
        h2.createCell(2).setCellValue("金额（万元）");
        h2.createCell(3).setCellValue("年份序号");
        for (var item : inv.items()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(item.category());
            row.createCell(1).setCellValue(item.name());
            numeric(row, 2, item.amount());
            row.createCell(3).setCellValue(item.yearNo());
        }
    }

    private void writeCashFlowSheet(Workbook workbook, List<CashFlowRow> cashFlowRows) {
        Sheet sheet = workbook.createSheet("现金流量表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("报表类型");
        header.createCell(1).setCellValue("期间序号");
        header.createCell(2).setCellValue("流入");
        header.createCell(3).setCellValue("流出");
        header.createCell(4).setCellValue("净现金流量");
        header.createCell(5).setCellValue("折现现金流量");
        header.createCell(6).setCellValue("累计现金流量");
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

    private void writeProfitFlowSheet(Workbook workbook, List<ProfitFlowResponse> profitFlow) {
        Sheet sheet = workbook.createSheet("利润流向");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("序号");
        header.createCell(1).setCellValue("项目");
        header.createCell(2).setCellValue("金额（万元）");
        for (int i = 0; i < profitFlow.size(); i++) {
            ProfitFlowResponse item = profitFlow.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(item.seq());
            row.createCell(1).setCellValue(item.label());
            numeric(row, 2, item.value());
        }
    }

    private void writeLoanScheduleSheet(Workbook workbook, List<LoanScheduleResponse> loanSchedule) {
        Sheet sheet = workbook.createSheet("还本付息");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("运营年");
        header.createCell(1).setCellValue("期初余额");
        header.createCell(2).setCellValue("还本");
        header.createCell(3).setCellValue("付息");
        header.createCell(4).setCellValue("期末余额");
        for (int i = 0; i < loanSchedule.size(); i++) {
            LoanScheduleResponse item = loanSchedule.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(item.yearNo());
            numeric(row, 1, item.openingBalance());
            numeric(row, 2, item.principalPaid());
            numeric(row, 3, item.interestPaid());
            numeric(row, 4, item.closingBalance());
        }
    }

    private int summaryRow(Sheet sheet, int r, String label, BigDecimal value) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(label);
        numeric(row, 1, value);
        return r + 1;
    }

    private void numeric(Row row, int column, BigDecimal value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.doubleValue());
        }
    }

    /** 基于核心指标的规则化投资建议（NPV>0 且 IRR>WACC 等口径在报告中以文字呈现）。 */
    static String buildAdvice(List<CalculationResultEntity> metrics) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        for (CalculationResultEntity e : metrics) {
            m.put(e.getMetricCode(), e.getMetricValue());
        }
        BigDecimal npv = m.get(MetricCodes.NPV);
        BigDecimal irr = m.get(MetricCodes.IRR);
        StringBuilder sb = new StringBuilder();
        boolean npvPositive = npv != null && npv.signum() > 0;
        if (npvPositive && irr != null) {
            sb.append("项目融资前净现值大于零，内部收益率 %s，财务上具备可行性；".formatted(irr.toPlainString()));
        } else if (npv != null) {
            sb.append("项目净现值不大于零，财务可行性存疑，建议优化投资与成本结构后重新测算；");
        }
        sb.append("本结论基于当前参数集与公式版本自动生成，供决策参考，不构成最终投资决定。");
        return sb.toString();
    }
}
