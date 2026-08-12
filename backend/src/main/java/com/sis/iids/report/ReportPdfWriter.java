package com.sis.iids.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;
import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CashFlowRow;
import com.sis.iids.calculation.LoanScheduleResponse;
import com.sis.iids.calculation.ProfitFlowResponse;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.MetricCodes;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF 报告 writer（D3 选型 A：OpenPDF）。
 * 使用 OpenPDF 内置 CJK 字体生成中文摘要。结构：封面信息（参数版本与数据来源引用）→
 * 指标摘要 → 投资估算 → 现金流量（融资前）→ 利润流向 → 还本付息。
 */
class ReportPdfWriter {

    private static final BaseFont CJK = cjkFont();
    private static final Font TITLE = new Font(CJK, 18, Font.BOLD);
    private static final Font H2 = new Font(CJK, 13, Font.BOLD);
    private static final Font BODY = new Font(CJK, 10);
    private static final Font SMALL = new Font(CJK, 8);

    void write(Path filePath, ReportContent c) {
        try {
            Files.createDirectories(filePath.getParent());
            try (OutputStream out = Files.newOutputStream(filePath)) {
                Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
                PdfWriter.getInstance(doc, out);
                doc.addTitle("投资回报分析报告");
                doc.addSubject("测算方案、核心指标、投资建议和现金流摘要");
                doc.open();
                writeCover(doc, c);
                writeMetrics(doc, c.metrics());
                writeInvestment(doc, c);
                writeCashFlow(doc, c.cashFlowRows());
                writeProfitFlow(doc, c.profitFlow());
                writeLoanSchedule(doc, c.loanSchedule());
                doc.close();
            }
        } catch (IOException | DocumentException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF 报表生成失败");
        }
    }

    private void writeCover(Document doc, ReportContent c) throws DocumentException {
        doc.add(new Paragraph("投资回报分析报告", TITLE));
        doc.add(new Paragraph(" ", BODY));
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(80);
        metaRow(t, "项目", c.project() == null ? "-" : c.project().getCode() + " " + c.project().getName());
        metaRow(t, "测算方案", c.scenario().getName() + "（版本 " + c.scenario().getVersionNo() + "）");
        metaRow(t, "评价期 / 建设期", c.scenario().getHorizonYears() + " 年 / " + c.scenario().getConstructionYears() + " 年");
        metaRow(t, "Task ID", String.valueOf(c.task().getId()));
        CalculationResultEntity first = c.metrics().get(0);
        metaRow(t, "Formula Version", first.getFormulaVersion());
        metaRow(t, "Engine Version", first.getEngineVersion());
        metaRow(t, "Parameter Set ID", c.params() == null ? "-" : String.valueOf(c.params().getId()));
        metaRow(t, "Input Hash", first.getInputHash() == null ? "-" : first.getInputHash());
        metaRow(t, "Generated At", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        doc.add(t);
        doc.add(new Paragraph(" ", BODY));
        Paragraph advice = new Paragraph("投资建议：" + ReportService.buildAdvice(c.metrics()), BODY);
        doc.add(advice);
        if (c.latestSensitivity() != null) {
            doc.add(new Paragraph("Latest sensitivity run #%d on %s (base value %s).".formatted(
                    c.latestSensitivity().getId(), c.latestSensitivity().getTargetMetric(),
                    c.latestSensitivity().getBaseValue() == null ? "-" : c.latestSensitivity().getBaseValue().toPlainString()), BODY));
        }
        doc.newPage();
    }

    private void writeMetrics(Document doc, List<CalculationResultEntity> metrics) throws DocumentException {
        doc.add(new Paragraph("1. 核心指标", H2));
        PdfPTable t = table(2, "指标", "数值");
        for (CalculationResultEntity m : metrics) {
            cell(t, m.getMetricCode());
            cell(t, fmt(m.getMetricValue()));
        }
        doc.add(t);
    }

    private void writeInvestment(Document doc, ReportContent c) throws DocumentException {
        doc.add(new Paragraph("2. 投资估算", H2));
        PdfPTable t = table(2, "项目", "金额");
        cell(t, "Construction Total");
        cell(t, fmt(c.investment().constructionTotal()));
        cell(t, "Interest During Construction");
        cell(t, fmt(c.investment().interestDuringConstruction()));
        cell(t, "Working Capital");
        cell(t, fmt(c.investment().workingCapital()));
        cell(t, "Total Investment");
        cell(t, fmt(c.investment().totalInvestment()));
        cell(t, "Balance Check");
        cell(t, c.investment().balanced() ? "BALANCED" : "UNBALANCED");
        doc.add(t);
    }

    private void writeCashFlow(Document doc, List<CashFlowRow> rows) throws DocumentException {
        doc.add(new Paragraph("3. 项目投资现金流量（融资前）", H2));
        PdfPTable t = table(5, "期次", "流入", "流出", "净现金流", "累计净现金流");
        rows.stream().filter(r -> MetricCodes.ST_PROJECT.equals(r.getStatementType())).forEach(r -> {
            cell(t, String.valueOf(r.getPeriodNo()));
            cell(t, fmt(r.getInflow()));
            cell(t, fmt(r.getOutflow()));
            cell(t, fmt(r.getNetCashFlow()));
            cell(t, fmt(r.getCumulativeCashFlow()));
        });
        doc.add(t);
    }

    private void writeProfitFlow(Document doc, List<ProfitFlowResponse> items) throws DocumentException {
        doc.add(new Paragraph("4. 利润流向（达产年）", H2));
        PdfPTable t = table(3, "序号", "项目", "金额");
        for (ProfitFlowResponse item : items) {
            cell(t, String.valueOf(item.seq()));
            cell(t, item.label());
            cell(t, fmt(item.value()));
        }
        doc.add(t);
    }

    private void writeLoanSchedule(Document doc, List<LoanScheduleResponse> items) throws DocumentException {
        doc.add(new Paragraph("5. 还本付息计划", H2));
        if (items.isEmpty()) {
            doc.add(new Paragraph("本方案无有息债务。", BODY));
            return;
        }
        PdfPTable t = table(5, "年度", "期初余额", "偿还本金", "支付利息", "期末余额");
        for (LoanScheduleResponse item : items) {
            cell(t, String.valueOf(item.yearNo()));
            cell(t, fmt(item.openingBalance()));
            cell(t, fmt(item.principalPaid()));
            cell(t, fmt(item.interestPaid()));
            cell(t, fmt(item.closingBalance()));
        }
        doc.add(t);
    }

    private void metaRow(PdfPTable t, String k, String v) {
        PdfPCell c1 = new PdfPCell(new Phrase(k, BODY));
        PdfPCell c2 = new PdfPCell(new Phrase(v, BODY));
        c1.setBorder(PdfPCell.NO_BORDER);
        c2.setBorder(PdfPCell.NO_BORDER);
        t.addCell(c1);
        t.addCell(c2);
    }

    private PdfPTable table(int cols, String... headers) {
        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6f);
        t.setSpacingAfter(12f);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, new Font(CJK, 9, Font.BOLD)));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBackgroundColor(new java.awt.Color(235, 238, 245));
            t.addCell(c);
        }
        return t;
    }

    private void cell(PdfPTable t, String v) {
        PdfPCell c = new PdfPCell(new Phrase(v, SMALL));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private String fmt(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }

    private static BaseFont cjkFont() {
        try {
            return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
