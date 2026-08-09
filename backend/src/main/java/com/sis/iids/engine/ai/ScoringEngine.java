package com.sis.iids.engine.ai;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * R-17 投资方案打分引擎（FR-05-03）。
 * 规则化加权评分（可解释，不替代人工决策）：六因子各自映射到 0~100 分，
 * 按模型权重加权求和；阈值 ≥70 建议立项，50~70 谨慎推进，<50 建议暂缓。
 *
 * <p>数值规范（红线 R2）：内部 MC(20, HALF_UP)，输出 scale=2。</p>
 */
public class ScoringEngine {

    private static final int SCALE = 2;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal THRESHOLD_RECOMMEND = new BigDecimal("70");
    private static final BigDecimal THRESHOLD_CAUTION = new BigDecimal("50");

    public static final String LABEL_RECOMMEND = "RECOMMEND";
    public static final String LABEL_CAUTION = "CAUTION";
    public static final String LABEL_HOLD = "HOLD";

    /**
     * @param features 特征
     * @param weights  权重配置（npv/irrSpread/payback/sensitivity/bepUtilization/reviewDeviation），缺省按 V14 种子
     */
    public ScoreOutcome score(ScoreFeatures features, Map<String, BigDecimal> weights) {
        List<ScoreOutcome.FactorScore> factors = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        total = total.add(factor(factors, "npv", "NPV 水平",
                features.npv() == null ? "—" : features.npv().toPlainString(),
                npvScore(features.npv()),
                weight(weights, "npv", "0.25"),
                npvExplain(features.npv())), MC);

        total = total.add(factor(factors, "irrSpread", "IRR-WACC 溢价",
                features.irr() == null ? "无解" : features.irr().toPlainString() + " vs " + features.wacc().toPlainString(),
                irrScore(features.irr(), features.wacc()),
                weight(weights, "irrSpread", "0.25"),
                irrExplain(features.irr(), features.wacc())), MC);

        total = total.add(factor(factors, "payback", "静态回收期",
                features.staticPayback() == null ? "—" : features.staticPayback().toPlainString() + " 年",
                paybackScore(features.staticPayback(), features.horizonYears()),
                weight(weights, "payback", "0.15"),
                paybackExplain(features.staticPayback(), features.horizonYears())), MC);

        total = total.add(factor(factors, "sensitivity", "敏感性等级",
                features.highestSensitivity() == null ? "未评估" : features.highestSensitivity(),
                sensitivityScore(features.highestSensitivity()),
                weight(weights, "sensitivity", "0.15"),
                sensitivityExplain(features.highestSensitivity())), MC);

        total = total.add(factor(factors, "bepUtilization", "盈亏平衡产能利用率",
                features.bepUtilization() == null ? "—" : features.bepUtilization().multiply(HUNDRED).setScale(1, RoundingMode.HALF_UP) + "%",
                bepScore(features.bepUtilization()),
                weight(weights, "bepUtilization", "0.10"),
                bepExplain(features.bepUtilization())), MC);

        total = total.add(factor(factors, "reviewDeviation", "历史复盘偏差",
                features.reviewDeviation() == null ? "无复盘数据" : features.reviewDeviation().multiply(HUNDRED).setScale(1, RoundingMode.HALF_UP) + "%",
                deviationScore(features.reviewDeviation()),
                weight(weights, "reviewDeviation", "0.10"),
                deviationExplain(features.reviewDeviation())), MC);

        BigDecimal totalScore = scale(total);
        String label = totalScore.compareTo(THRESHOLD_RECOMMEND) >= 0 ? LABEL_RECOMMEND
                : totalScore.compareTo(THRESHOLD_CAUTION) >= 0 ? LABEL_CAUTION : LABEL_HOLD;
        return new ScoreOutcome(totalScore, label, factors);
    }

    // ============================================================
    // 因子评分映射（0~100）
    // ============================================================
    /** NPV：≤0 → 0；线性至 NPV/总投资收益率代理（此处按绝对额分档，服务层可扩展相对口径）。 */
    private BigDecimal npvScore(BigDecimal npv) {
        if (npv == null) {
            return new BigDecimal("30");
        }
        if (npv.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        // 分档：>20万 → 100；>10万 → 85；>5万 → 70；>1万 → 50；否则 30
        if (npv.compareTo(new BigDecimal("200000")) > 0) {
            return HUNDRED;
        }
        if (npv.compareTo(new BigDecimal("100000")) > 0) {
            return new BigDecimal("85");
        }
        if (npv.compareTo(new BigDecimal("50000")) > 0) {
            return new BigDecimal("70");
        }
        if (npv.compareTo(new BigDecimal("10000")) > 0) {
            return new BigDecimal("50");
        }
        return new BigDecimal("30");
    }

    private String npvExplain(BigDecimal npv) {
        if (npv == null) {
            return "NPV 缺失，按中性偏低 30 分";
        }
        if (npv.signum() <= 0) {
            return "NPV ≤ 0，项目不创造价值，0 分";
        }
        return "NPV 为正值，按绝对额分档映射（>20万=100，>10万=85，>5万=70，>1万=50）";
    }

    /** IRR-WACC 溢价：IRR 无解 → 0；<WACC → 10；溢价 ≥10pct → 100 线性递减。 */
    private BigDecimal irrScore(BigDecimal irr, BigDecimal wacc) {
        if (irr == null || wacc == null) {
            return new BigDecimal("20");
        }
        BigDecimal spread = irr.subtract(wacc, MC);
        if (spread.signum() < 0) {
            return new BigDecimal("10");
        }
        // 溢价 0 → 40 分；溢价 10pct → 100 分；线性
        BigDecimal score = new BigDecimal("40").add(spread.multiply(new BigDecimal("600"), MC), MC);
        return score.min(HUNDRED);
    }

    private String irrExplain(BigDecimal irr, BigDecimal wacc) {
        if (irr == null) {
            return "IRR 无解（现金流无变号），20 分";
        }
        if (wacc == null) {
            return "WACC 缺失，按 20 分";
        }
        BigDecimal spread = irr.subtract(wacc, MC);
        if (spread.signum() < 0) {
            return "IRR 低于 WACC，无法覆盖资金成本，10 分";
        }
        return "IRR 超出 WACC " + spread.multiply(HUNDRED).setScale(1, RoundingMode.HALF_UP)
                + " 个百分点，线性映射（0pct=40，≥10pct=100）";
    }

    /** 回收期：≤测算期 40% → 100；≥测算期 → 0；线性。 */
    private BigDecimal paybackScore(BigDecimal payback, int horizonYears) {
        if (payback == null || horizonYears <= 0) {
            return new BigDecimal("30");
        }
        BigDecimal horizon = BigDecimal.valueOf(horizonYears);
        if (payback.compareTo(horizon) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ratio = payback.divide(horizon, MC);
        if (ratio.compareTo(new BigDecimal("0.4")) <= 0) {
            return HUNDRED;
        }
        // 0.4 → 100；1.0 → 0；线性
        return HUNDRED.subtract(ratio.subtract(new BigDecimal("0.4"), MC)
                .divide(new BigDecimal("0.6"), MC).multiply(HUNDRED, MC), MC).max(BigDecimal.ZERO);
    }

    private String paybackExplain(BigDecimal payback, int horizonYears) {
        if (payback == null) {
            return "回收期缺失，按 30 分";
        }
        return "回收期占测算期比例映射（≤40%=100，≥100%=0，线性递减）";
    }

    private BigDecimal sensitivityScore(String level) {
        if (level == null) {
            return new BigDecimal("50");
        }
        return switch (level) {
            case "LOW" -> HUNDRED;
            case "MEDIUM" -> new BigDecimal("60");
            case "HIGH" -> new BigDecimal("20");
            default -> new BigDecimal("50");
        };
    }

    private String sensitivityExplain(String level) {
        if (level == null) {
            return "未做敏感性分析，按中性 50 分";
        }
        return "最高敏感等级 " + level + "（LOW=100 / MEDIUM=60 / HIGH=20）";
    }

    /** BEP 产能利用率：≤50% → 100；≥100% → 0；线性。 */
    private BigDecimal bepScore(BigDecimal utilization) {
        if (utilization == null) {
            return new BigDecimal("40");
        }
        if (utilization.compareTo(new BigDecimal("0.5")) <= 0) {
            return HUNDRED;
        }
        if (utilization.compareTo(BigDecimal.ONE) >= 0) {
            return BigDecimal.ZERO;
        }
        return HUNDRED.subtract(utilization.subtract(new BigDecimal("0.5"), MC)
                .multiply(new BigDecimal("200"), MC), MC).max(BigDecimal.ZERO);
    }

    private String bepExplain(BigDecimal utilization) {
        if (utilization == null) {
            return "未做盈亏平衡分析，按 40 分";
        }
        return "BEP 产能利用率越低抗风险越强（≤50%=100，≥100%=0，线性递减）";
    }

    /** 复盘偏差率绝对值：≤5% → 100；≥30% → 20；线性。 */
    private BigDecimal deviationScore(BigDecimal deviation) {
        if (deviation == null) {
            return new BigDecimal("60");
        }
        BigDecimal abs = deviation.abs();
        if (abs.compareTo(new BigDecimal("0.05")) <= 0) {
            return HUNDRED;
        }
        if (abs.compareTo(new BigDecimal("0.30")) >= 0) {
            return new BigDecimal("20");
        }
        return HUNDRED.subtract(abs.subtract(new BigDecimal("0.05"), MC)
                .divide(new BigDecimal("0.25"), MC).multiply(new BigDecimal("80"), MC), MC);
    }

    private String deviationExplain(BigDecimal deviation) {
        if (deviation == null) {
            return "无历史复盘数据，按中性 60 分";
        }
        return "历史复盘偏差率绝对值映射（≤5%=100，≥30%=20，线性递减）";
    }

    // ============================================================
    // 内部
    // ============================================================
    private BigDecimal factor(List<ScoreOutcome.FactorScore> factors, String code, String name,
                              String rawValue, BigDecimal score, BigDecimal weight, String explain) {
        BigDecimal s = scale(score);
        BigDecimal weighted = scale(s.multiply(weight, MC));
        factors.add(new ScoreOutcome.FactorScore(code, name, rawValue, s, weight, weighted, explain));
        return weighted;
    }

    private BigDecimal weight(Map<String, BigDecimal> weights, String key, String defaultValue) {
        if (weights == null) {
            return new BigDecimal(defaultValue);
        }
        return weights.getOrDefault(key, new BigDecimal(defaultValue));
    }

    private BigDecimal scale(BigDecimal v) {
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
