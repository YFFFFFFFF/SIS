package com.sis.iids.engine.financial;

/**
 * 贷款还本付息方式。
 */
public enum RepaymentMethod {
    /** 等额本金 */
    EQUAL_PRINCIPAL,
    /** 等额本息 */
    EQUAL_PAYMENT,
    /** 到期一次还本（运营期付息） */
    BULLET
}
