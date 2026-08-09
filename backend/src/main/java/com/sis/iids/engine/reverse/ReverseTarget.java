package com.sis.iids.engine.reverse;

/**
 * 目标反算的目标指标（FR-01-05）。
 */
public enum ReverseTarget {
    /** 净现值（万元），目标值语义：使 NPV 达到 targetValue */
    NPV,
    /** 内部收益率（小数，如 0.12），目标值语义：使 IRR 达到 targetValue */
    IRR,
    /** 静态投资回收期（年），目标值语义：使回收期不超过 targetValue */
    STATIC_PAYBACK_YEARS;

    public static ReverseTarget from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("目标指标不能为空");
        }
        try {
            return ReverseTarget.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的目标指标: " + raw + "（支持 NPV/IRR/STATIC_PAYBACK_YEARS）");
        }
    }
}
