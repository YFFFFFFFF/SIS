package com.sis.iids.engine.financial;

import java.math.BigDecimal;

/**
 * 利润流向分解节点（FR-01-02，达产年口径，供瀑布图/桑基图直接取数）。
 * key 取值见设计文档 §6.4：REVENUE / OPERATING_TAX_SURTAX / OPERATING_COST /
 * DEPRECIATION_AMORTIZATION / FINANCE_COST / PROFIT_BEFORE_TAX / INCOME_TAX / NET_PROFIT。
 */
public record ProfitFlowItem(int seq, String key, String label, BigDecimal value) {
}
