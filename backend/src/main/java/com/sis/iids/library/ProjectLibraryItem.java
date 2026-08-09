package com.sis.iids.library;

import java.math.BigDecimal;
import java.util.List;

/**
 * R-16 项目库检索条目（FR-03-03）：项目 + 结构化标签 + 最新测算摘要。
 */
public record ProjectLibraryItem(Long id,
                                 String code,
                                 String name,
                                 String projectType,
                                 String status,
                                 String department,
                                 List<String> tags,
                                 String description,
                                 BigDecimal latestNpv,
                                 BigDecimal latestIrr,
                                 boolean hasReview) {
}
