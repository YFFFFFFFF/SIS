package com.sis.iids.engine.financial;

import java.math.BigDecimal;

/**
 * 建设投资分项（FR-01-01）：category 见设计文档 §3.2（CONSTRUCTION_* 二级类别）。
 */
public record InvestmentEntry(String category, String name, BigDecimal amount) {
}
