package com.sis.iids.calculation;

import java.math.BigDecimal;

/**
 * 利润流向分解节点（FR-01-02，达产年口径，供瀑布图/桑基图直接取数）。
 */
public record ProfitFlowResponse(int seq, String key, String label, BigDecimal value) {
}
