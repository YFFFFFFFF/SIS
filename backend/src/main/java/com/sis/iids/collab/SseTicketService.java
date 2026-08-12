package com.sis.iids.collab;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseTicketService {
    private static final long VALID_SECONDS = 60;
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public SseTicketResponse issue(Long scenarioId, String username) {
        cleanupExpired();
        String value = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(VALID_SECONDS);
        tickets.put(value, new Ticket(scenarioId, username, expiresAt));
        return new SseTicketResponse(value, expiresAt);
    }

    public void consume(String value, Long scenarioId) {
        Ticket ticket = value == null ? null : tickets.remove(value);
        if (ticket == null || ticket.expiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("SSE 连接凭证无效或已过期");
        }
        if (!ticket.scenarioId().equals(scenarioId)) {
            throw new AccessDeniedException("SSE 连接凭证与测算方案不匹配");
        }
    }

    private void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record Ticket(Long scenarioId, String username, LocalDateTime expiresAt) {
    }
}
