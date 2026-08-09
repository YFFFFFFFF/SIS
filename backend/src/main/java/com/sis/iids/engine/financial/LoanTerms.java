package com.sis.iids.engine.financial;

import java.math.BigDecimal;

/**
 * 贷款条款。principalRatioOfConstruction 为贷款占建设投资比例（提款与分年投资同步）。
 * repaymentYears 含宽限期；graceYears 内只付息。
 */
public class LoanTerms {

    private BigDecimal principalRatioOfConstruction = BigDecimal.ZERO;
    private BigDecimal interestRate = BigDecimal.ZERO;
    private int repaymentYears = 0;
    private int graceYears = 0;
    private RepaymentMethod repaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL;

    public BigDecimal getPrincipalRatioOfConstruction() {
        return principalRatioOfConstruction;
    }

    public void setPrincipalRatioOfConstruction(BigDecimal principalRatioOfConstruction) {
        this.principalRatioOfConstruction = principalRatioOfConstruction;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public int getRepaymentYears() {
        return repaymentYears;
    }

    public void setRepaymentYears(int repaymentYears) {
        this.repaymentYears = repaymentYears;
    }

    public int getGraceYears() {
        return graceYears;
    }

    public void setGraceYears(int graceYears) {
        this.graceYears = graceYears;
    }

    public RepaymentMethod getRepaymentMethod() {
        return repaymentMethod;
    }

    public void setRepaymentMethod(RepaymentMethod repaymentMethod) {
        this.repaymentMethod = repaymentMethod;
    }
}
