package com.sis.iids.engine.breakeven;

import com.sis.iids.engine.financial.CostEntry;
import com.sis.iids.engine.financial.FinancialEngine;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.FinancialResult;
import com.sis.iids.engine.financial.StatementRow;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 盈亏平衡分析引擎（FR-02-02）。
 * 基于达产年口径的成本性态分解（可变 vs 固定），计算三口径盈亏平衡点：
 * BEP 产量 = 年固定成本 ÷ 单位边际贡献；产能利用率 = BEP 产量 ÷ 设计产量；
 * 盈亏平衡售价 = 单位可变成本 + 年固定成本 ÷ 设计产量。
 *
 * <p>数值规范（红线 R2）：内部 MC(20, HALF_UP)，输出 scale=4。</p>
 *
 * <p>假设声明（约束：需标注适用边界与假设）：
 * 采用税前会计口径（经营成本 + 折旧摊销，不含财务费用与所得税），
 * 成本性态按 RAW_MATERIAL=可变、其余经营成本+折旧+摊销=固定 分解，
 * 达产年（负荷=100%）口径，未考虑投产爬坡与年度覆盖的年间差异。</p>
 */
public class BreakEvenEngine {

    private static final int SCALE = 4;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int CURVE_POINTS = 11;

    private final FinancialEngine financialEngine = new FinancialEngine();

    /**
     * @param input 基准方案输入（服务层装配）
     */
    public BreakEvenResult analyze(FinancialInput input) {
        BigDecimal price = nz(input.getPricePerUnit());
        BigDecimal output = nz(input.getAnnualOutput());
        BigDecimal unitVar = nz(input.getUnitVariableCost());

        // 固定成本 = 达产年固定经营成本（非 RAW_MATERIAL、yearNo=0）+ 达产年折旧 + 达产年摊销
        BigDecimal fixedOperating = BigDecimal.ZERO;
        for (CostEntry entry : input.getCostEntries()) {
            if ("RAW_MATERIAL".equals(entry.category())) {
                continue;
            }
            if (entry.yearNo() == 0) {
                fixedOperating = fixedOperating.add(entry.amount(), MC);
            }
        }
        BigDecimal depAmort = firstOperationYearDepAmort(input);
        BigDecimal fixed = fixedOperating.add(depAmort, MC);

        BigDecimal contribution = price.subtract(unitVar, MC);
        boolean solvable = contribution.signum() > 0 && output.signum() > 0;
        String unsolvableReason = null;
        BigDecimal bepOutput = null;
        BigDecimal bepUtilization = null;
        BigDecimal bepPrice = null;
        if (solvable) {
            bepOutput = scale(fixed.divide(contribution, MC));
            bepUtilization = scale(bepOutput.divide(output, MC));
            bepPrice = scale(unitVar.add(fixed.divide(output, MC), MC));
        } else if (output.signum() <= 0) {
            unsolvableReason = "达产年产量为 0，无法计算盈亏平衡点";
        } else {
            unsolvableReason = "单位边际贡献（售价 − 单位可变成本 = " + scale(contribution)
                    + "）≤ 0，售价无法覆盖可变成本，任何产量下均亏损";
        }

        List<BreakEvenResult.CurvePoint> curve = buildCurve(price, unitVar, fixed, output);

        return new BreakEvenResult(
                scale(price), scale(output), scale(unitVar), scale(fixed),
                bepOutput, bepUtilization, bepPrice, scale(contribution),
                solvable, unsolvableReason, curve, assumptionNote());
    }

    /** 第一个运营年的折旧+摊销（达产年口径）。引擎不可算时回退为 0（如无建设投资）。 */
    private BigDecimal firstOperationYearDepAmort(FinancialInput input) {
        try {
            FinancialResult result = financialEngine.calculate(input);
            int firstOpYear = input.getConstructionYears() + 1;
            for (StatementRow row : result.projectRows()) {
                if (row.getPeriodNo() == firstOpYear) {
                    return nz(row.getDepreciation()).add(nz(row.getAmortization()), MC);
                }
            }
            // 回退：直接按直线法估算（引擎行缺失时兜底）
            return straightLineDep(input).add(firstYearAmort(input), MC);
        } catch (RuntimeException ex) {
            return straightLineDep(input).add(firstYearAmort(input), MC);
        }
    }

    /** 直线法年折旧兜底估算（不含建设期利息资本化，仅用于引擎不可算时） */
    private BigDecimal straightLineDep(FinancialInput input) {
        BigDecimal base = input.constructionInvestment();
        if (base.signum() <= 0 || input.getDepreciationYears() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal residual = nz(input.getResidualRate());
        return base.multiply(BigDecimal.ONE.subtract(residual, MC), MC)
                .divide(BigDecimal.valueOf(input.getDepreciationYears()), MC);
    }

    private BigDecimal firstYearAmort(FinancialInput input) {
        if (input.getAmortizationYears() > 0 && nz(input.getAmortizableAmount()).signum() > 0) {
            return input.getAmortizableAmount().divide(BigDecimal.valueOf(input.getAmortizationYears()), MC);
        }
        return BigDecimal.ZERO;
    }

    /** 盈亏平衡图数据点：产量 0 ~ 150% 设计产量，11 个点（含 BEP 附近交叉区） */
    private List<BreakEvenResult.CurvePoint> buildCurve(BigDecimal price, BigDecimal unitVar,
                                                        BigDecimal fixed, BigDecimal output) {
        List<BreakEvenResult.CurvePoint> points = new ArrayList<>();
        BigDecimal maxOutput = output.signum() > 0
                ? output.multiply(new BigDecimal("1.5"), MC)
                : new BigDecimal("1000");
        for (int i = 0; i < CURVE_POINTS; i++) {
            BigDecimal ratio = BigDecimal.valueOf(i).divide(BigDecimal.valueOf(CURVE_POINTS - 1), MC);
            BigDecimal q = maxOutput.multiply(ratio, MC);
            BigDecimal revenue = price.multiply(q, MC);
            BigDecimal totalCost = fixed.add(unitVar.multiply(q, MC), MC);
            points.add(new BreakEvenResult.CurvePoint(scale(q), scale(revenue), scale(totalCost)));
        }
        return points;
    }

    private String assumptionNote() {
        return "适用边界与假设：1) 口径为达产年（负荷 100%）税前会计口径——总成本 = 经营成本 + 折旧 + 摊销，"
                + "不含财务费用与所得税，BEP 对应税前利润为 0 的产量水平；"
                + "2) 成本性态分解：外购原材料及燃料动力（RAW_MATERIAL）按可变成本随产量线性变化，"
                + "其余经营成本与折旧摊销按固定成本处理；"
                + "3) 未考虑投产爬坡、年度成本覆盖与多产品结构的年间差异；"
                + "4) 盈亏平衡图为收入线/总成本线随产量的线性关系，交点即 BEP 产量。";
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
