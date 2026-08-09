package com.sis.iids.engine.portfolio;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 投资组合优化引擎（FR-03-02，D1 选型 A：oj! Algorithms）。
 * 0-1 整数规划：在资金池（与可选数量上限）硬约束下，选取候选方案子集使组合 NPV 最大化。
 *
 * <p>模型：max Σ npvᵢ·xᵢ，s.t. Σ invᵢ·xᵢ ≤ budget，Σ xᵢ ≤ maxCount（可选），xᵢ ∈ {0,1}。</p>
 * <p>帕累托前沿：按预算档位（0%~100% 资金池，21 档）逐档求解，输出（预算 → 最优 NPV）单调曲线。</p>
 * <p>数值规范（红线 R2）：内部 MC(20, HALF_UP)，输出 scale=4。</p>
 */
public class PortfolioEngine {

    private static final int SCALE = 4;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int FRONTIER_STEPS = 21;

    /**
     * @param candidates 候选方案（NPV/总投资均已测算）
     * @param budget     资金池上限（必须 > 0）
     * @param maxCount   数量上限（null 或 ≤ 0 = 不限）
     */
    public PortfolioResult optimize(List<PortfolioCandidate> candidates, BigDecimal budget, Integer maxCount) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("候选方案不能为空");
        }
        if (budget == null || budget.signum() <= 0) {
            throw new IllegalArgumentException("资金池预算必须为正数");
        }
        int limit = (maxCount == null || maxCount <= 0) ? candidates.size() : Math.min(maxCount, candidates.size());

        SolveOutcome best = solve(candidates, budget, limit);
        List<PortfolioResult.FrontierPoint> frontier = buildFrontier(candidates, budget, limit);
        String explanation = buildExplanation(candidates, best, budget, limit);

        List<Long> selected = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            if (best.selection[i]) {
                selected.add(candidates.get(i).scenarioId());
            }
        }
        // 按 NPV 降序输出
        selected.sort(Comparator.comparing((Long id) -> candidates.stream()
                        .filter(c -> c.scenarioId().equals(id)).findFirst().orElseThrow().npv(),
                Comparator.reverseOrder()));

        return new PortfolioResult(selected, scale(best.npv), scale(best.investment),
                scale(budget), maxCount, explanation, frontier);
    }

    // ============================================================
    // 求解
    // ============================================================
    private SolveOutcome solve(List<PortfolioCandidate> candidates, BigDecimal budget, int maxCount) {
        int n = candidates.size();
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable[] xs = new Variable[n];
        for (int i = 0; i < n; i++) {
            PortfolioCandidate c = candidates.get(i);
            xs[i] = Variable.make("x" + i).lower(0).upper(1).integer(true)
                    .weight(c.npv().negate());   // oj! 默认最小化 → 取负实现最大化
            model.addVariable(xs[i]);
        }
        // 资金约束 Σ invᵢ·xᵢ ≤ budget
        var budgetExpr = model.addExpression("budget").upper(budget.doubleValue());
        for (int i = 0; i < n; i++) {
            budgetExpr.set(xs[i], candidates.get(i).investment().doubleValue());
        }
        // 数量约束 Σ xᵢ ≤ maxCount
        var countExpr = model.addExpression("count").upper((double) maxCount);
        for (int i = 0; i < n; i++) {
            countExpr.set(xs[i], 1.0);
        }

        Optimisation.Result result = model.minimise();
        if (!result.getState().isFeasible()) {
            throw new IllegalArgumentException("组合优化无可行解（约束过紧或候选数据异常）");
        }
        boolean[] selection = new boolean[n];
        double npv = 0;
        double investment = 0;
        for (int i = 0; i < n; i++) {
            selection[i] = result.get(i).compareTo(BigDecimal.valueOf(0.5)) > 0;
            if (selection[i]) {
                npv += candidates.get(i).npv().doubleValue();
                investment += candidates.get(i).investment().doubleValue();
            }
        }
        return new SolveOutcome(selection, npv, investment);
    }

    /** 帕累托前沿：预算 0% ~ 100% 资金池 21 档逐档求解（忽略不可行档）。 */
    private List<PortfolioResult.FrontierPoint> buildFrontier(List<PortfolioCandidate> candidates,
                                                              BigDecimal budget, int maxCount) {
        List<PortfolioResult.FrontierPoint> points = new ArrayList<>();
        for (int i = 0; i < FRONTIER_STEPS; i++) {
            BigDecimal ratio = BigDecimal.valueOf(i).divide(BigDecimal.valueOf(FRONTIER_STEPS - 1), MC);
            BigDecimal b = budget.multiply(ratio, MC);
            if (b.signum() <= 0) {
                points.add(new PortfolioResult.FrontierPoint(scale(b), BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP), 0));
                continue;
            }
            try {
                SolveOutcome outcome = solve(candidates, b, maxCount);
                int count = 0;
                for (boolean s : outcome.selection) {
                    if (s) {
                        count++;
                    }
                }
                points.add(new PortfolioResult.FrontierPoint(scale(b), scale(outcome.npv), scale(outcome.investment), count));
            } catch (IllegalArgumentException ex) {
                // 该预算档无可行解（如所有方案投资均超该档预算）→ 记零点
                points.add(new PortfolioResult.FrontierPoint(scale(b), BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP), 0));
            }
        }
        return points;
    }

    /** 求解解释：选中/未选中原因、资金利用率、边际价值（最后一个入选者的 NPV/投资比）。 */
    private String buildExplanation(List<PortfolioCandidate> candidates, SolveOutcome best,
                                    BigDecimal budget, int maxCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("目标函数：组合 NPV 最大化；约束：总投资 ≤ ").append(scale(budget))
                .append("，数量 ≤ ").append(maxCount).append("。");
        List<String> selectedNames = new ArrayList<>();
        List<String> excludedNames = new ArrayList<>();
        BigDecimal minEfficiency = null;
        for (int i = 0; i < candidates.size(); i++) {
            PortfolioCandidate c = candidates.get(i);
            if (best.selection[i]) {
                selectedNames.add(c.scenarioName());
                if (c.investment().signum() > 0) {
                    BigDecimal eff = c.npv().divide(c.investment(), MC);
                    if (minEfficiency == null || eff.compareTo(minEfficiency) < 0) {
                        minEfficiency = eff;
                    }
                }
            } else {
                excludedNames.add(c.scenarioName());
            }
        }
        sb.append("入选 ").append(selectedNames.size()).append(" 个：").append(String.join("、", selectedNames)).append("。");
        if (!excludedNames.isEmpty()) {
            sb.append("未入选 ").append(excludedNames.size()).append(" 个：").append(String.join("、", excludedNames))
                    .append("（资金/数量约束下边际价值低于入选方案）。");
        }
        if (budget.signum() > 0) {
            BigDecimal utilization = BigDecimal.valueOf(best.investment).divide(budget, MC)
                    .multiply(BigDecimal.valueOf(100), MC);
            sb.append("资金利用率 ").append(utilization.setScale(1, RoundingMode.HALF_UP)).append("%");
            if (minEfficiency != null) {
                sb.append("；组合边际 NPV/投资比 ≈ ").append(minEfficiency.setScale(SCALE, RoundingMode.HALF_UP))
                        .append("（追加资金若投向未入选方案，单位资金 NPV 贡献不高于该值）");
            }
            sb.append("。");
        }
        return sb.toString();
    }

    private BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(double v) {
        return BigDecimal.valueOf(v).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private record SolveOutcome(boolean[] selection, double npv, double investment) {
    }
}
