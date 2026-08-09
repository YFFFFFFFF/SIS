package com.sis.iids.engine.montecarlo;

/**
 * 蒙特卡洛抽样变量（FR-02-03）。
 * 抽样值以比例扰动表示（如 TRIANGULAR(-0.2, 0, 0.3) 表示基准的 -20%~+30%，最可能 0），
 * 作用于基准输入后重算（与敏感性分析同一套 apply 语义）。
 */
public enum MonteCarloVariable {
    /** 产品售价（pricePerUnit） */
    PRICE,
    /** 单位成本（unitVariableCost + RAW_MATERIAL 成本分项） */
    UNIT_COST,
    /** 建设投资（constructionEntries 同比例缩放） */
    INVESTMENT,
    /** 年产量（annualOutput） */
    ANNUAL_OUTPUT;

    public static MonteCarloVariable from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("蒙特卡洛变量不能为空");
        }
        try {
            return MonteCarloVariable.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的蒙特卡洛变量: " + raw);
        }
    }
}
