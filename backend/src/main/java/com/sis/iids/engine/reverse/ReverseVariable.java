package com.sis.iids.engine.reverse;

/**
 * 目标反算的被反算变量（FR-01-05：售价/投资额/产量/单位成本之一）。
 * 以比例因子 factor 作用于基准值（实际值 = 基准 × factor），二分求解 factor。
 */
public enum ReverseVariable {
    /** 产品售价（pricePerUnit） */
    PRICE,
    /** 建设投资（各 constructionEntries 同比例缩放） */
    INVESTMENT,
    /** 年产量（annualOutput） */
    ANNUAL_OUTPUT,
    /** 单位可变成本（unitVariableCost） */
    UNIT_COST;

    public static ReverseVariable from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("反算变量不能为空");
        }
        try {
            return ReverseVariable.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的反算变量: " + raw + "（支持 PRICE/INVESTMENT/ANNUAL_OUTPUT/UNIT_COST）");
        }
    }
}
