package com.sis.iids.engine.financial;

import java.math.BigDecimal;

/**
 * 还本付息计划表行（k 为运营年序号，1 起）。
 */
public class LoanScheduleRow {

    private int yearNo;
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal principalPaid = BigDecimal.ZERO;
    private BigDecimal interestPaid = BigDecimal.ZERO;
    private BigDecimal closingBalance = BigDecimal.ZERO;

    public int getYearNo() {
        return yearNo;
    }

    public void setYearNo(int yearNo) {
        this.yearNo = yearNo;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    public void setPrincipalPaid(BigDecimal principalPaid) {
        this.principalPaid = principalPaid;
    }

    public BigDecimal getInterestPaid() {
        return interestPaid;
    }

    public void setInterestPaid(BigDecimal interestPaid) {
        this.interestPaid = interestPaid;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }
}
