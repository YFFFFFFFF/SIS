package com.sis.iids.audit;

import java.util.List;

/**
 * R-08 审计链校验结果。
 *
 * @param totalEvents     事件总数
 * @param linkedEvents    已纳入哈希链的事件数（V6 之后写入）
 * @param intact          链是否完整（无篡改迹象）
 * @param brokenCount     校验失败的事件数
 * @param brokenEventIds  校验失败的事件 ID（hash 重算不符或 prev_hash 断链）
 */
public record AuditChainVerification(int totalEvents, int linkedEvents, boolean intact,
                                     int brokenCount, List<Long> brokenEventIds) {
}
