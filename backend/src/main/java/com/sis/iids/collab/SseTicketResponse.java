package com.sis.iids.collab;

import java.time.LocalDateTime;

public record SseTicketResponse(String ticket, LocalDateTime expiresAt) {
}
