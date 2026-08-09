package com.sis.iids.engine.financial;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务评价引擎 v2（设计文档 §5）。
 * 符合《建设项目经济评价方法与参数》（第三版）口径：
 * 分年建设流出、建设期利息资本化（当年借款半额计息）、三种折旧政策、摊销、
 * 三种还本付息方式、税率梯度、投产期负荷、三类现金流量表、利润流向分解。
 *
 * <p>无状态纯函数（红线 R1）：calculate 可被敏感性/蒙特卡洛批量重算调用（§8.5）。
 * 数值规范（红线 R2）：内部 MathContext(20, HALF_UP)，输出 scale=4。</p>
 */
public class FinancialEngine {

    private static final int SCALE = 4;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal SCHEDULE_SUM_TOLERANCE = new BigDecimal("0.00000001");

    public FinancialResult calculate(FinancialInput input) {
        validate(input);

        int cY = input.getConstructionYears();
        int oY = input.getOperationYears();
        BigDecimal wacc = input.getWacc();

        // ---- 投资与建设期（§5.1/§5.2）----
        BigDecimal constructionTotal = input.constructionInvestment();
        BigDecimal wc = input.getWorkingCapital();
        BigDecimal loanRatio = input.getLoan() == null ? BigDecimal.ZERO
                : input.getLoan().getPrincipalRatioOfConstruction();

        BigDecimal[] constructionOut = new BigDecimal[cY];      // 分年建设投资流出
        BigDecimal[] idc = new BigDecimal[cY];                  // 分年建设期利息（资本化）
        BigDecimal loanBalance = BigDecimal.ZERO;
        BigDecimal idcTotal = BigDecimal.ZERO;
        BigDecimal loanRate = input.getLoan() == null ? BigDecimal.ZERO : input.getLoan().getInterestRate();
        for (int t = 0; t < cY; t++) {
            constructionOut[t] = constructionTotal.multiply(input.getConstructionSchedule().get(t), MC);
            BigDecimal draw = constructionOut[t].multiply(loanRatio, MC);   // 当年提款
            // 当年借款半额计息惯例
            idc[t] = loanBalance.add(draw.multiply(new BigDecimal("0.5"), MC), MC).multiply(loanRate, MC);
            loanBalance = loanBalance.add(draw, MC).add(idc[t], MC);
            idcTotal = idcTotal.add(idc[t], MC);
        }
        BigDecimal principalAtOperation = loanBalance;   // 运营期初本金（含资本化利息）
        BigDecimal totalInvestment = constructionTotal.add(idcTotal, MC).add(wc, MC);

        // ---- 折旧 / 摊销（§5.3/§5.4）----
        BigDecimal depreciationBase = constructionTotal.add(idcTotal, MC);
        List<BigDecimal> depSeq = DepreciationCalculator.schedule(
                input.getDepreciationPolicy(), depreciationBase, input.getResidualRate(),
                input.getDepreciationYears(), oY);
        List<BigDecimal> amortSeq = new ArrayList<>();
        for (int k = 1; k <= oY; k++) {
            BigDecimal a = (input.getAmortizationYears() > 0 && k <= input.getAmortizationYears())
                    ? input.getAmortizableAmount().divide(BigDecimal.valueOf(input.getAmortizationYears()), 12, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            amortSeq.add(a);
        }

        // ---- 还本付息计划（§5.5）----
        List<LoanScheduleRow> loanSchedule = input.getLoan() == null ? List.of()
                : LoanAmortizationCalculator.schedule(principalAtOperation, input.getLoan());
        Map<Integer, LoanScheduleRow> loanByYear = new HashMap<>();
        for (LoanScheduleRow row : loanSchedule) {
            loanByYear.put(row.getYearNo(), row);
        }

        // ---- 运营年利润（§5.6）----
        BigDecimal[] rev = new BigDecimal[oY];
        BigDecimal[] opC = new BigDecimal[oY];
        BigDecimal[] interest = new BigDecimal[oY];
        BigDecimal[] pbt = new BigDecimal[oY];
        BigDecimal[] tax = new BigDecimal[oY];
        BigDecimal[] np = new BigDecimal[oY];
        BigDecimal[] ebit = new BigDecimal[oY];
        BigDecimal[] adjTax = new BigDecimal[oY];
        BigDecimal[] principalPaid = new BigDecimal[oY];

        for (int k = 1; k <= oY; k++) {
            int i = k - 1;
            BigDecimal f = loadFactor(input, k);
            rev[i] = input.getPricePerUnit().multiply(input.getAnnualOutput(), MC).multiply(f, MC);
            BigDecimal varCost = input.getUnitVariableCost().multiply(input.getAnnualOutput(), MC).multiply(f, MC);
            BigDecimal fixCost = fixedCost(input, k);
            opC[i] = varCost.add(fixCost, MC);
            LoanScheduleRow loanRow = loanByYear.get(k);
            interest[i] = loanRow == null ? BigDecimal.ZERO : loanRow.getInterestPaid();
            principalPaid[i] = loanRow == null ? BigDecimal.ZERO : loanRow.getPrincipalPaid();
            pbt[i] = rev[i].subtract(opC[i], MC).subtract(depSeq.get(i), MC)
                    .subtract(amortSeq.get(i), MC).subtract(interest[i], MC);
            BigDecimal rate = taxRate(input, k);
            tax[i] = positive(pbt[i]).multiply(rate, MC);
            np[i] = pbt[i].subtract(tax[i], MC);
            ebit[i] = pbt[i].add(interest[i], MC);
            adjTax[i] = positive(ebit[i]).multiply(rate, MC);
        }

        // ---- 三类现金流量表（§5.7）----
        Map<String, List<StatementRow>> statements = new LinkedHashMap<>();
        statements.put(MetricCodes.ST_PROJECT,
                buildProjectRows(cY, oY, constructionOut, rev, opC, adjTax, wc, wacc,
                        depSeq, amortSeq, interest, tax, np));
        statements.put(MetricCodes.ST_EQUITY,
                buildEquityRows(cY, oY, constructionOut, idc, rev, opC, tax, principalPaid, interest,
                        wc, loanRatio, wacc, depSeq, amortSeq, np));
        statements.put(MetricCodes.ST_PLAN,
                buildPlanRows(cY, oY, constructionOut, idc, rev, opC, tax, principalPaid, interest,
                        wc, loanRatio, wacc, depSeq, amortSeq, np));

        // ---- 指标（§5.8 / §7.1）----
        Map<String, BigDecimal> metrics = new LinkedHashMap<>();
        metrics.put(MetricCodes.TOTAL_INVESTMENT, scale(totalInvestment));
        metrics.put(MetricCodes.CONSTRUCTION_INTEREST, scale(idcTotal));
        List<StatementRow> projectRows = statements.get(MetricCodes.ST_PROJECT);
        List<StatementRow> equityRows = statements.get(MetricCodes.ST_EQUITY);
        metrics.put(MetricCodes.NPV, scale(npv(projectRows, wacc)));
        putIfNotNull(metrics, MetricCodes.IRR, irr(projectRows));
        metrics.put(MetricCodes.STATIC_PAYBACK_YEARS, scale(payback(projectRows, false)));
        metrics.put(MetricCodes.DYNAMIC_PAYBACK_YEARS, scale(payback(projectRows, true)));
        metrics.put(MetricCodes.ROI, scale(safeDivide(average(ebit), totalInvestment)));
        BigDecimal capital = totalInvestment.multiply(input.getEquityRatio(), MC);
        metrics.put(MetricCodes.CAPITAL_NET_PROFIT_RATE, scale(safeDivide(average(np), capital)));
        putIfNotNull(metrics, MetricCodes.EQUITY_IRR, irr(equityRows));
        metrics.put(MetricCodes.EQUITY_NPV, scale(npv(equityRows, wacc)));

        FinancialResult result = new FinancialResult();
        result.setTotalInvestment(scale(totalInvestment));
        result.setConstructionInterest(scale(idcTotal));
        result.setStatements(statements);
        result.setProfitFlow(buildProfitFlow(input, oY, rev, opC, depSeq, amortSeq, interest, pbt, tax, np));
        result.setLoanSchedule(loanSchedule);
        result.setMetrics(metrics);
        return result;
    }

    // ============================================================
    // 校验（设计文档 §4.2，V4/V9 在服务层，引擎执行 V1~V3、V5~V8）
    // ============================================================
    private void validate(FinancialInput input) {
        if (input.getConstructionYears() <= 0 || input.getOperationYears() <= 0) {
            throw new IllegalArgumentException("建设期和运营期必须为正数");
        }
        List<BigDecimal> schedule = input.getConstructionSchedule();
        if (schedule.size() != input.getConstructionYears()) {
            throw new IllegalArgumentException("建设投资分年进度数量必须与建设期年数一致");
        }
        BigDecimal sum = schedule.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.subtract(BigDecimal.ONE).abs().compareTo(SCHEDULE_SUM_TOLERANCE) > 0) {
            throw new IllegalArgumentException("建设投资分年进度合计必须等于 1");
        }
        if (input.constructionInvestment().signum() <= 0) {
            throw new IllegalArgumentException("建设投资分项合计必须大于 0");
        }
        if (input.getLoan() != null && input.getLoan().getGraceYears() >= input.getLoan().getRepaymentYears()) {
            throw new IllegalArgumentException("宽限期必须小于还款年限");
        }
        // 税率梯度区间不重叠
        List<TaxBracket> brackets = new ArrayList<>(input.getTaxSchedule());
        brackets.sort((a, b) -> Integer.compare(a.fromYear(), b.fromYear()));
        for (int i = 1; i < brackets.size(); i++) {
            if (brackets.get(i).fromYear() <= brackets.get(i - 1).toYear()) {
                throw new IllegalArgumentException("税率梯度区间不允许重叠");
            }
        }
        for (RampUpYear r : input.getRampUp()) {
            if (r.year() < 1 || r.year() > input.getOperationYears()
                    || r.loadFactor().signum() <= 0 || r.loadFactor().compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("投产负荷年份或负荷因子不合法");
            }
        }
        if (input.getDepreciationYears() <= 0) {
            throw new IllegalArgumentException("折旧年限必须为正数");
        }
        if (input.getDepreciationPolicy() != DepreciationPolicy.STRAIGHT_LINE
                && input.getDepreciationYears() > input.getOperationYears()) {
            throw new IllegalArgumentException("加速折旧政策下折旧年限不能超过运营期年数");
        }
    }

    // ============================================================
    // 三类报表构建
    // ============================================================

    /** (a) 项目投资现金流量表（融资前，所得税后，调整所得税口径） */
    private List<StatementRow> buildProjectRows(int cY, int oY, BigDecimal[] constructionOut,
                                                BigDecimal[] rev, BigDecimal[] opC, BigDecimal[] adjTax,
                                                BigDecimal wc, BigDecimal wacc,
                                                List<BigDecimal> dep, List<BigDecimal> amort,
                                                BigDecimal[] interest, BigDecimal[] tax, BigDecimal[] np) {
        List<StatementRow> rows = new ArrayList<>();
        for (int t = 0; t < cY; t++) {
            rows.add(row(t, BigDecimal.ZERO, constructionOut[t], wacc));
        }
        for (int k = 1; k <= oY; k++) {
            int i = k - 1;
            int t = cY + i;
            BigDecimal inflow = rev[i];
            BigDecimal outflow = opC[i].add(adjTax[i], MC);
            if (k == 1) {
                outflow = outflow.add(wc, MC);              // 流动资金投产年投入
            }
            if (k == oY) {
                inflow = inflow.add(wc, MC);                // 末年回收流动资金
            }
            StatementRow row = row(t, inflow, outflow, wacc);
            fillBreakdown(row, rev[i], opC[i], dep.get(i), amort.get(i), interest[i], tax[i], np[i]);
            rows.add(row);
        }
        applyCumulativeAndDiscount(rows, wacc);
        return rows;
    }

    /** (b) 项目资本金现金流量表（融资后） */
    private List<StatementRow> buildEquityRows(int cY, int oY, BigDecimal[] constructionOut, BigDecimal[] idc,
                                               BigDecimal[] rev, BigDecimal[] opC, BigDecimal[] tax,
                                               BigDecimal[] principalPaid, BigDecimal[] interest,
                                               BigDecimal wc, BigDecimal loanRatio, BigDecimal wacc,
                                               List<BigDecimal> dep, List<BigDecimal> amort, BigDecimal[] np) {
        List<StatementRow> rows = new ArrayList<>();
        BigDecimal equityShare = BigDecimal.ONE.subtract(loanRatio, MC);
        for (int t = 0; t < cY; t++) {
            BigDecimal outflow = constructionOut[t].add(idc[t], MC).multiply(equityShare, MC);
            rows.add(row(t, BigDecimal.ZERO, outflow, wacc));
        }
        BigDecimal wcEquity = wc.multiply(equityShare, MC);
        for (int k = 1; k <= oY; k++) {
            int i = k - 1;
            int t = cY + i;
            BigDecimal inflow = rev[i];
            BigDecimal outflow = opC[i].add(tax[i], MC).add(principalPaid[i], MC).add(interest[i], MC);
            if (k == 1) {
                outflow = outflow.add(wcEquity, MC);
            }
            if (k == oY) {
                inflow = inflow.add(wcEquity, MC);
            }
            StatementRow row = row(t, inflow, outflow, wacc);
            fillBreakdown(row, rev[i], opC[i], dep.get(i), amort.get(i), interest[i], tax[i], np[i]);
            rows.add(row);
        }
        applyCumulativeAndDiscount(rows, wacc);
        return rows;
    }

    /** (c) 财务计划现金流量表（资金来源与运用） */
    private List<StatementRow> buildPlanRows(int cY, int oY, BigDecimal[] constructionOut,
                                             BigDecimal[] idc, BigDecimal[] rev, BigDecimal[] opC, BigDecimal[] tax,
                                             BigDecimal[] principalPaid, BigDecimal[] interest,
                                             BigDecimal wc, BigDecimal loanRatio, BigDecimal wacc,
                                             List<BigDecimal> dep, List<BigDecimal> amort, BigDecimal[] np) {
        List<StatementRow> rows = new ArrayList<>();
        BigDecimal equityShare = BigDecimal.ONE.subtract(loanRatio, MC);
        for (int t = 0; t < cY; t++) {
            // 视同提款含资本化利息中贷款部分，使来源 = 运用（资本金 + 提款 + IDC 贷款部分 = 投资 + IDC 付息）
            BigDecimal deemedDrawdown = constructionOut[t].add(idc[t], MC).multiply(loanRatio, MC);
            BigDecimal equity = constructionOut[t].add(idc[t], MC).multiply(equityShare, MC);
            BigDecimal inflow = equity.add(deemedDrawdown, MC);
            BigDecimal outflow = constructionOut[t].add(idc[t], MC);
            rows.add(row(t, inflow, outflow, wacc));
        }
        for (int k = 1; k <= oY; k++) {
            int i = k - 1;
            int t = cY + i;
            BigDecimal inflow = rev[i];
            BigDecimal outflow = opC[i].add(tax[i], MC).add(principalPaid[i], MC).add(interest[i], MC);
            if (k == 1) {
                outflow = outflow.add(wc, MC);
            }
            if (k == oY) {
                inflow = inflow.add(wc, MC);
            }
            StatementRow row = row(t, inflow, outflow, wacc);
            fillBreakdown(row, rev[i], opC[i], dep.get(i), amort.get(i), interest[i], tax[i], np[i]);
            rows.add(row);
        }
        applyCumulativeAndDiscount(rows, wacc);
        return rows;
    }

    // ============================================================
    // 利润流向分解（§6.4，达产年 = 首个负荷因子为 1.0 的运营年）
    // ============================================================
    private List<ProfitFlowItem> buildProfitFlow(FinancialInput input, int oY,
                                                 BigDecimal[] rev, BigDecimal[] opC,
                                                 List<BigDecimal> dep, List<BigDecimal> amort,
                                                 BigDecimal[] interest, BigDecimal[] pbt,
                                                 BigDecimal[] tax, BigDecimal[] np) {
        int kStar = 1;
        for (int k = 1; k <= oY; k++) {
            if (loadFactor(input, k).compareTo(BigDecimal.ONE) == 0) {
                kStar = k;
                break;
            }
        }
        int i = kStar - 1;
        List<ProfitFlowItem> items = new ArrayList<>();
        items.add(new ProfitFlowItem(1, "REVENUE", "营业收入", scale(rev[i])));
        items.add(new ProfitFlowItem(2, "OPERATING_TAX_SURTAX", "税金及附加", scale(BigDecimal.ZERO)));
        items.add(new ProfitFlowItem(3, "OPERATING_COST", "经营成本", scale(opC[i])));
        items.add(new ProfitFlowItem(4, "DEPRECIATION_AMORTIZATION", "折旧与摊销",
                scale(dep.get(i).add(amort.get(i), MC))));
        items.add(new ProfitFlowItem(5, "FINANCE_COST", "财务费用", scale(interest[i])));
        items.add(new ProfitFlowItem(6, "PROFIT_BEFORE_TAX", "利润总额", scale(pbt[i])));
        items.add(new ProfitFlowItem(7, "INCOME_TAX", "所得税", scale(tax[i])));
        items.add(new ProfitFlowItem(8, "NET_PROFIT", "净利润", scale(np[i])));
        return items;
    }

    // ============================================================
    // 取数辅助
    // ============================================================

    /** 运营年 k 的负荷因子：rampUp 命中取配置值，否则 1.0 */
    private BigDecimal loadFactor(FinancialInput input, int k) {
        for (RampUpYear r : input.getRampUp()) {
            if (r.year() == k) {
                return r.loadFactor();
            }
        }
        return BigDecimal.ONE;
    }

    /** 运营年 k 的固定经营成本：yearNo=k 覆盖优先，否则 yearNo=0 达产默认（§5.6） */
    private BigDecimal fixedCost(FinancialInput input, int k) {
        BigDecimal sum = BigDecimal.ZERO;
        for (CostEntry entry : input.getCostEntries()) {
            if ("RAW_MATERIAL".equals(entry.category())) {
                continue;   // 可变成本走 unitVariableCost 通道
            }
            if (entry.yearNo() == k) {
                sum = sum.add(entry.amount(), MC);
            } else if (entry.yearNo() == 0 && !hasOverride(input, entry.category(), k)) {
                sum = sum.add(entry.amount(), MC);
            }
        }
        return sum;
    }

    private boolean hasOverride(FinancialInput input, String category, int k) {
        for (CostEntry entry : input.getCostEntries()) {
            if (category.equals(entry.category()) && entry.yearNo() == k) {
                return true;
            }
        }
        return false;
    }

    /** 运营年 k 的适用税率：税率梯度命中取区间值，否则基准税率（§5.6） */
    private BigDecimal taxRate(FinancialInput input, int k) {
        for (TaxBracket b : input.getTaxSchedule()) {
            if (k >= b.fromYear() && k <= b.toYear()) {
                return b.rate();
            }
        }
        return input.getTaxRate();
    }

    // ============================================================
    // 行构建 / 折现 / 指标
    // ============================================================

    private StatementRow row(int periodNo, BigDecimal inflow, BigDecimal outflow, BigDecimal discountRate) {
        StatementRow row = new StatementRow();
        row.setPeriodNo(periodNo);
        row.setInflow(scale(inflow));
        row.setOutflow(scale(outflow));
        row.setNetCashFlow(scale(inflow.subtract(outflow, MC)));
        return row;
    }

    private void fillBreakdown(StatementRow row, BigDecimal rev, BigDecimal opC, BigDecimal dep,
                               BigDecimal amort, BigDecimal interest, BigDecimal tax, BigDecimal np) {
        row.setRevenue(scale(rev));
        row.setOperatingCost(scale(opC));
        row.setDepreciation(scale(dep));
        row.setAmortization(scale(amort));
        row.setInterest(scale(interest));
        row.setTax(scale(tax));
        row.setNetProfit(scale(np));
    }

    /** 折现与累计（在行净现金流落定后统一处理） */
    private void applyCumulativeAndDiscount(List<StatementRow> rows, BigDecimal wacc) {
        BigDecimal cumulative = BigDecimal.ZERO;
        for (StatementRow row : rows) {
            BigDecimal discounted = discount(row.getNetCashFlow(), wacc, row.getPeriodNo());
            row.setDiscountedCashFlow(scale(discounted));
            cumulative = cumulative.add(row.getNetCashFlow(), MC);
            row.setCumulativeCashFlow(scale(cumulative));
        }
    }

    private BigDecimal npv(List<StatementRow> rows, BigDecimal wacc) {
        BigDecimal npv = BigDecimal.ZERO;
        for (StatementRow row : rows) {
            npv = npv.add(discount(row.getNetCashFlow(), wacc, row.getPeriodNo()), MC);
        }
        return npv;
    }

    /** IRR 二分求解；无解（无变号）返回 null（红线：禁止 0 占位） */
    private BigDecimal irr(List<StatementRow> rows) {
        double low = -0.9999d;
        double high = 10.0d;
        double lowNpv = npvAtRate(rows, low);
        double highNpv = npvAtRate(rows, high);
        if (Double.isNaN(lowNpv) || Double.isNaN(highNpv) || lowNpv * highNpv > 0) {
            return null;
        }
        for (int i = 0; i < 100; i++) {
            double mid = (low + high) / 2.0d;
            double midNpv = npvAtRate(rows, mid);
            if (Math.abs(midNpv) < 0.000001d) {
                return scale(BigDecimal.valueOf(mid));
            }
            if (lowNpv * midNpv > 0) {
                low = mid;
                lowNpv = midNpv;
            } else {
                high = mid;
            }
        }
        return scale(BigDecimal.valueOf((low + high) / 2.0d));
    }

    private double npvAtRate(List<StatementRow> rows, double rate) {
        double npv = 0.0d;
        for (StatementRow row : rows) {
            npv += row.getNetCashFlow().doubleValue() / Math.pow(1.0d + rate, row.getPeriodNo());
        }
        return npv;
    }

    /** 回收期（含建设期）：累计（折现）净现金流由负转非负的年份插值 */
    private BigDecimal payback(List<StatementRow> rows, boolean discounted) {
        BigDecimal cumulative = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            StatementRow row = rows.get(i);
            BigDecimal value = discounted ? row.getDiscountedCashFlow() : row.getNetCashFlow();
            BigDecimal next = cumulative.add(value, MC);
            if (cumulative.signum() < 0 && next.signum() >= 0) {
                BigDecimal deficit = cumulative.abs();
                return BigDecimal.valueOf(i - 1L).add(safeDivide(deficit, value), MC);
            }
            cumulative = next;
        }
        return BigDecimal.valueOf(rows.size() - 1L);
    }

    private BigDecimal average(BigDecimal[] values) {
        if (values.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            sum = sum.add(v, MC);
        }
        return sum.divide(BigDecimal.valueOf(values.length), 12, RoundingMode.HALF_UP);
    }

    private BigDecimal discount(BigDecimal value, BigDecimal rate, int periodNo) {
        if (periodNo == 0 || rate == null || rate.signum() == 0) {
            return value;
        }
        BigDecimal factor = BigDecimal.ONE.add(rate, MC).pow(periodNo, MC);
        return value.divide(factor, 20, RoundingMode.HALF_UP);
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 12, RoundingMode.HALF_UP);
    }

    private BigDecimal positive(BigDecimal value) {
        return value.signum() > 0 ? value : BigDecimal.ZERO;
    }

    private void putIfNotNull(Map<String, BigDecimal> metrics, String code, BigDecimal value) {
        if (value != null) {
            metrics.put(code, scale(value));
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
