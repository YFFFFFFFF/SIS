package com.sis.iids.engine.reverse;

import com.sis.iids.engine.financial.FinancialEngine;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.FinancialResult;
import com.sis.iids.engine.financial.InvestmentEntry;
import com.sis.iids.engine.financial.MetricCodes;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 目标反算求解引擎（FR-01-05）。
 * 复用无状态 {@link FinancialEngine}（红线 R1/R9）：给定目标指标目标值，
 * 对被反算变量的比例因子做二分迭代，直至目标指标与目标值的偏差收敛或迭代耗尽。
 *
 * <p>数值规范（红线 R2）：内部 MC(20, HALF_UP)，输出 scale=4。</p>
 *
 * <p>假设声明（约束"需标注适用边界与假设"）：
 * 仅单调方向上二分有效——售价/产量与投资指标同向，单位成本/投资额与投资指标反向；
 * 搜索区间为基准值的 [1%, 1000%]，目标超出该区间能达成的范围时返回 feasible=false。</p>
 */
public class ReverseEngine {

    private static final int SCALE = 4;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int MAX_ITERATIONS = 60;
    /** 收敛容差：目标指标绝对偏差（NPV 万元 / IRR 小数 / 回收期 年） */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.0001");
    private static final BigDecimal FACTOR_MIN = new BigDecimal("0.01");
    private static final BigDecimal FACTOR_MAX = BigDecimal.TEN;

    private final FinancialEngine financialEngine = new FinancialEngine();

    /**
     * @param baseInput   基准方案输入（其余已知变量保持基准，服务层装配）
     * @param target      目标指标
     * @param targetValue 目标值（NPV 万元；IRR 小数；回收期 年）
     * @param variable    被反算变量
     */
    public ReverseResult solve(FinancialInput baseInput, ReverseTarget target,
                               BigDecimal targetValue, ReverseVariable variable) {
        if (targetValue == null) {
            throw new IllegalArgumentException("目标值不能为空");
        }
        String metric = metricCode(target);
        BigDecimal baseMetric = metricOf(financialEngine.calculate(baseInput), metric);
        if (baseMetric == null) {
            throw new IllegalArgumentException("基准方案目标指标 " + metric + " 无解（如 IRR 无变号现金流），无法反算");
        }

        // 目标达成方向：指标值随 factor 增大是升还是降（决定二分取舍方向）。
        // 端点可能无解（如投资趋零时 IRR 无变号现金流），向内探测有效边界。
        BigDecimal lo = FACTOR_MIN;
        BigDecimal hi = FACTOR_MAX;
        BigDecimal metricAtLo = metricOf(calculate(baseInput, variable, lo), metric);
        BigDecimal metricAtHi = metricOf(calculate(baseInput, variable, hi), metric);
        for (int probe = 0; probe < 8 && metricAtLo == null; probe++) {
            lo = lo.multiply(BigDecimal.TEN, MC).min(FACTOR_MAX);
            metricAtLo = metricOf(calculate(baseInput, variable, lo), metric);
        }
        for (int probe = 0; probe < 8 && metricAtHi == null; probe++) {
            hi = hi.divide(BigDecimal.TEN, MC).max(FACTOR_MIN);
            metricAtHi = metricOf(calculate(baseInput, variable, hi), metric);
        }
        if (metricAtLo == null || metricAtHi == null) {
            return infeasible(target, targetValue, variable, baseMetric,
                    "区间内存在无解点（如 IRR 不存在），请调整基准方案或目标值");
        }

        // 目标值是否落在 [metricAtLo, metricAtHi] 覆盖范围内（回收期按“不超过目标年”语义判定）
        BigDecimal rangeLo = metricAtLo.min(metricAtHi);
        BigDecimal rangeHi = metricAtLo.max(metricAtHi);
        boolean outOfReach = target == ReverseTarget.STATIC_PAYBACK_YEARS
                ? targetValue.compareTo(rangeLo) < 0
                : (targetValue.compareTo(rangeLo) < 0 || targetValue.compareTo(rangeHi) > 0);
        if (outOfReach) {
            return infeasible(target, targetValue, variable, baseMetric,
                    "目标值超出变量可调范围（基准的 1%%~1000%%）能达成的指标区间 [%s, %s]，请修正目标或放宽假设"
                            .formatted(rangeLo.setScale(SCALE, RoundingMode.HALF_UP),
                                    rangeHi.setScale(SCALE, RoundingMode.HALF_UP)));
        }
        // 升序（指标随 factor 增大而增大）为 true；降序为 false
        boolean ascending = metricAtHi.compareTo(metricAtLo) > 0;

        BigDecimal bestFactor = BigDecimal.ONE;
        BigDecimal bestMetric = baseMetric;
        int iterations = 0;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            iterations = i + 1;
            BigDecimal mid = lo.add(hi, MC).divide(BigDecimal.valueOf(2), MC);
            BigDecimal metricAtMid = metricOf(calculate(baseInput, variable, mid), metric);
            if (metricAtMid == null) {
                // 中点无解（如 IRR 不存在的区段），向低区收缩重试
                hi = mid;
                continue;
            }
            // 已满足目标语义即收敛（NPV/IRR 达到目标值；回收期不超过目标年）
            if (meetsTarget(target, metricAtMid, targetValue)) {
                bestFactor = mid;
                bestMetric = metricAtMid;
                break;
            }
            bestFactor = mid;
            bestMetric = metricAtMid;
            boolean needHigher = metricAtMid.compareTo(targetValue) < 0;
            if (needHigher == ascending) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        boolean feasible = meetsTarget(target, bestMetric, targetValue);

        BigDecimal solvedValue = scale(baseValueOf(baseInput, variable).multiply(bestFactor, MC));
        if (!feasible) {
            return infeasible(target, targetValue, variable, baseMetric,
                    "区间内未找到满足目标语义的解（目标语义：" + targetSemantic(target) + "）");
        }
        return new ReverseResult(metric, scale(targetValue), variable.name(), scale(bestFactor),
                solvedValue, scale(baseMetric), scale(bestMetric), true, iterations,
                sensitivityNote(variable, baseMetric, bestMetric, target),
                boundaryNote(variable, target));
    }

    /** 目标语义判定：NPV/IRR 为“达到目标值”（绝对偏差收敛），回收期为“不超过目标年”。 */
    private boolean meetsTarget(ReverseTarget target, BigDecimal metric, BigDecimal targetValue) {
        if (metric == null) {
            return false;
        }
        if (target == ReverseTarget.STATIC_PAYBACK_YEARS) {
            return metric.compareTo(targetValue) <= 0;
        }
        return metric.subtract(targetValue, MC).abs().compareTo(TOLERANCE.multiply(BigDecimal.valueOf(100), MC)) <= 0;
    }

    private String targetSemantic(ReverseTarget target) {
        return switch (target) {
            case NPV -> "使 NPV 达到目标值";
            case IRR -> "使 IRR 达到目标值";
            case STATIC_PAYBACK_YEARS -> "使静态回收期不超过目标年";
        };
    }

    private ReverseResult infeasible(ReverseTarget target, BigDecimal targetValue, ReverseVariable variable,
                                     BigDecimal baseMetric, String reason) {
        return new ReverseResult(metricCode(target), scale(targetValue), variable.name(), null, null,
                scale(baseMetric), null, false, 0,
                "未在搜索区间内求得可行解",
                "适用边界与假设：" + reason + "。基准指标=" + scale(baseMetric));
    }

    private String sensitivityNote(ReverseVariable variable, BigDecimal baseMetric,
                                   BigDecimal achievedMetric, ReverseTarget target) {
        String direction = switch (variable) {
            case PRICE, ANNUAL_OUTPUT -> "同向（变量增大则指标改善）";
            case INVESTMENT, UNIT_COST -> "反向（变量增大则指标恶化）";
        };
        BigDecimal change = achievedMetric.subtract(baseMetric, MC);
        return "%s 对 %s 的影响为 %s；从基准 %s 调整至 %s（Δ=%s），变量每变动 1%% 指标约变动 %s。"
                .formatted(variableName(variable), targetName(target), direction,
                        baseMetric.setScale(SCALE, RoundingMode.HALF_UP),
                        achievedMetric.setScale(SCALE, RoundingMode.HALF_UP),
                        change.setScale(SCALE, RoundingMode.HALF_UP),
                        elasticityHint(variable));
    }

    private String elasticityHint(ReverseVariable variable) {
        return switch (variable) {
            case PRICE -> "较高（售价通常为最敏感因素）";
            case ANNUAL_OUTPUT -> "中等";
            case UNIT_COST -> "中等偏负";
            case INVESTMENT -> "偏负且主要在建设期";
        };
    }

    private String boundaryNote(ReverseVariable variable, ReverseTarget target) {
        return "适用边界与假设：1) 仅调整 %s，其余参数（含 WACC、税率、折旧政策、投产负荷、融资结构）保持基准不变；"
                .formatted(variableName(variable))
                + "2) 求解基于二分法，依赖指标对该变量的单调性，搜索区间为基准值的 1%~1000%；"
                + "3) 反算结果为临界参考值，不构成决策唯一依据，极端取值需结合市场与工程可行性复核。";
    }

    // ---------- 内部工具 ----------

    private FinancialResult calculate(FinancialInput base, ReverseVariable variable, BigDecimal factor) {
        FinancialInput copy = copyOf(base);
        apply(copy, variable, factor);
        return financialEngine.calculate(copy);
    }

    private void apply(FinancialInput input, ReverseVariable variable, BigDecimal factor) {
        switch (variable) {
            case PRICE -> input.setPricePerUnit(input.getPricePerUnit().multiply(factor, MC));
            case ANNUAL_OUTPUT -> input.setAnnualOutput(input.getAnnualOutput().multiply(factor, MC));
            case UNIT_COST -> input.setUnitVariableCost(input.getUnitVariableCost().multiply(factor, MC));
            case INVESTMENT -> {
                List<InvestmentEntry> scaled = new ArrayList<>();
                for (InvestmentEntry e : input.getConstructionEntries()) {
                    scaled.add(new InvestmentEntry(e.category(), e.name(), e.amount().multiply(factor, MC)));
                }
                input.setConstructionEntries(scaled);
            }
        }
    }

    private BigDecimal baseValueOf(FinancialInput input, ReverseVariable variable) {
        return switch (variable) {
            case PRICE -> input.getPricePerUnit();
            case ANNUAL_OUTPUT -> input.getAnnualOutput();
            case UNIT_COST -> input.getUnitVariableCost();
            case INVESTMENT -> input.getConstructionEntries().stream()
                    .map(InvestmentEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        };
    }

    private BigDecimal metricOf(FinancialResult result, String metric) {
        return result.getMetrics().get(metric);
    }

    private String metricCode(ReverseTarget target) {
        return switch (target) {
            case NPV -> MetricCodes.NPV;
            case IRR -> MetricCodes.IRR;
            case STATIC_PAYBACK_YEARS -> MetricCodes.STATIC_PAYBACK_YEARS;
        };
    }

    private String targetName(ReverseTarget target) {
        return switch (target) {
            case NPV -> "NPV";
            case IRR -> "IRR";
            case STATIC_PAYBACK_YEARS -> "静态回收期";
        };
    }

    private String variableName(ReverseVariable variable) {
        return switch (variable) {
            case PRICE -> "产品售价";
            case ANNUAL_OUTPUT -> "年产量";
            case UNIT_COST -> "单位成本";
            case INVESTMENT -> "建设投资";
        };
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 浅拷贝 + 集合深拷贝（与 SensitivityEngine 口径一致，保证克隆不影响基准）。 */
    private FinancialInput copyOf(FinancialInput s) {
        FinancialInput c = new FinancialInput();
        c.setConstructionYears(s.getConstructionYears());
        c.setOperationYears(s.getOperationYears());
        c.setWacc(s.getWacc());
        c.setWaccSource(s.getWaccSource());
        c.setPricePerUnit(s.getPricePerUnit());
        c.setAnnualOutput(s.getAnnualOutput());
        c.setRampUp(new ArrayList<>(s.getRampUp()));
        c.setConstructionEntries(new ArrayList<>(s.getConstructionEntries()));
        c.setConstructionSchedule(new ArrayList<>(s.getConstructionSchedule()));
        c.setWorkingCapital(s.getWorkingCapital());
        c.setAmortizableAmount(s.getAmortizableAmount());
        c.setAmortizationYears(s.getAmortizationYears());
        c.setCostEntries(new ArrayList<>(s.getCostEntries()));
        c.setUnitVariableCost(s.getUnitVariableCost());
        c.setDepreciationPolicy(s.getDepreciationPolicy());
        c.setDepreciationYears(s.getDepreciationYears());
        c.setResidualRate(s.getResidualRate());
        c.setTaxRate(s.getTaxRate());
        c.setTaxSchedule(new ArrayList<>(s.getTaxSchedule()));
        c.setEquityRatio(s.getEquityRatio());
        c.setLoan(s.getLoan());
        return c;
    }
}
