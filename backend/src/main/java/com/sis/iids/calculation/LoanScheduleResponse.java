package com.sis.iids.calculation;

import java.math.BigDecimal;

/**
 * 还本付息计划表行（k 为运营年序号，1 起）。
 */
public record LoanScheduleResponse(int yearNo, BigDecimal openingBalance, BigDecimal principalPaid,
                                   BigDecimal interestPaid, BigDecimal closingBalance) {
}
