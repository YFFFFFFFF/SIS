package com.sis.iids.engine.financial;

import java.math.BigDecimal;

/**
 * 成本费用分项（FR-01-02）。
 * yearNo = 0 表示达产年默认值；yearNo &gt; 0 表示该运营年覆盖值。
 * category：RAW_MATERIAL（可变，随负荷缩放）/ LABOR_MANUFACTURING / OTHER_OPERATING（固定）。
 */
public record CostEntry(String category, String name, int yearNo, BigDecimal amount) {
}
