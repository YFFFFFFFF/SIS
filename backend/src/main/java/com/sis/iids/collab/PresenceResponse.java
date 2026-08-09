package com.sis.iids.collab;

import java.time.LocalDateTime;

/**
 * R-15 在线用户（FR-04-02）。
 */
public record PresenceResponse(Long userId,
                               String userName,
                               LocalDateTime lastSeenAt) {
}
