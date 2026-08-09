package com.sis.iids.engine.sensitivity;

/**
 * 敏感性分析变量（FR-02-01）。
 * 波动以比例表示（如 -0.20 = 下浮 20%），作用于基准输入后重算。
 */
public enum SensitivityVariable {
    /** 产品售价（pricePerUnit） */
    PRICE,
    /** 单位成本（unitVariableCost，即可变成本） */
    UNIT_COST,
    /** 建设投资（各 constructionEntries 同比例缩放） */
    INVESTMENT,
    /** 建设工期（constructionYears，±整数年，按步进取整） */
    CONSTRUCTION_PERIOD;

    public static SensitivityVariable from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("敏感性变量不能为空");
        }
        try {
            return SensitivityVariable.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的敏感性变量: " + raw);
        }
    }
}
