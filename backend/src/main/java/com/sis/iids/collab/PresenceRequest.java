package com.sis.iids.collab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * R-15 心跳请求（FR-04-02）。
 */
public record PresenceRequest(@NotNull Long userId,
                              @NotBlank String userName) {
}
