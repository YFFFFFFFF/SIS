package com.sis.iids.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * R-17 智能参数推荐响应（FR-05-02）：推荐值/区间 + 依据来源（可解释）。
 */
public record ParamRecommendationResponse(Long scenarioId,
                                          List<Item> items,
                                          String basisSummary) {

    /**
     * @param param     参数编码（wacc / pricePerUnit / unitCost / sensitivityRange）
     * @param current   当前取值
     * @param recommendedLow  建议区间下限
     * @param recommendedHigh 建议区间上限
     * @param basis     依据来源（历史复盘偏差/蒙特卡洛波动/基准假设）
     */
    public record Item(String param,
                       BigDecimal current,
                       BigDecimal recommendedLow,
                       BigDecimal recommendedHigh,
                       String basis) {
    }
}
