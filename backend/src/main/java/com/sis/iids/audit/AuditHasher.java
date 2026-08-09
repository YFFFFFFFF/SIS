package com.sis.iids.audit;

import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * R-08 审计链式哈希工具（FR-04-03 约束"日志不可篡改"）。
 * 内容域：action|targetType|targetId|beforeValue|afterValue|traceId|prevHash，
 * null 一律以空串参与计算，保证写入与校验两侧口径一致。
 */
public final class AuditHasher {

    /** 首条事件的 prev_hash 占位值 */
    public static final String GENESIS = "GENESIS";

    private AuditHasher() {
    }

    public static String hash(AuditEvent event) {
        String content = String.join("|",
                nullToEmpty(event.getAction()),
                nullToEmpty(event.getTargetType()),
                nullToEmpty(event.getTargetId()),
                nullToEmpty(event.getBeforeValue()),
                nullToEmpty(event.getAfterValue()),
                nullToEmpty(event.getTraceId()),
                nullToEmpty(event.getPrevHash()));
        return sha256(content);
    }

    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "SHA-256 算法不可用");
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
