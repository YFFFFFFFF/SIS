package com.sis.iids.engine.financial;

/**
 * 折旧政策（FR-01-02 约束：可选且可审计）。
 */
public enum DepreciationPolicy {
    /** 年限平均法 */
    STRAIGHT_LINE,
    /** 双倍余额递减法 */
    DOUBLE_DECLINING,
    /** 年数总和法 */
    SUM_OF_YEARS_DIGITS
}
