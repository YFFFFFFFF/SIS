package com.sis.iids.engine.financial;

/**
 * 指标编码与报表类型常量（设计文档 §7.1 / §3.2），供引擎、服务层与测试共用。
 */
public final class MetricCodes {

    private MetricCodes() {
    }

    /** 报表类型：项目投资现金流量表（融资前） */
    public static final String ST_PROJECT = "PROJECT_CASH_FLOW";
    /** 报表类型：项目资本金现金流量表（融资后） */
    public static final String ST_EQUITY = "EQUITY_CASH_FLOW";
    /** 报表类型：财务计划现金流量表 */
    public static final String ST_PLAN = "FINANCIAL_PLAN";

    public static final String TOTAL_INVESTMENT = "TOTAL_INVESTMENT";
    public static final String CONSTRUCTION_INTEREST = "CONSTRUCTION_INTEREST";
    public static final String NPV = "NPV";
    public static final String IRR = "IRR";
    public static final String STATIC_PAYBACK_YEARS = "STATIC_PAYBACK_YEARS";
    public static final String DYNAMIC_PAYBACK_YEARS = "DYNAMIC_PAYBACK_YEARS";
    public static final String ROI = "ROI";
    public static final String CAPITAL_NET_PROFIT_RATE = "CAPITAL_NET_PROFIT_RATE";
    public static final String EQUITY_IRR = "EQUITY_IRR";
    public static final String EQUITY_NPV = "EQUITY_NPV";
}
