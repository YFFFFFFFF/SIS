package com.sis.iids.collaboration;

import jakarta.validation.constraints.NotNull;

public record ReleaseLockRequest(@NotNull Long holderId) {
}