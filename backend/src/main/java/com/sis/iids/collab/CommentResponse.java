package com.sis.iids.collab;

import java.time.LocalDateTime;

/**
 * R-15 评论响应（FR-04-02）。
 */
public record CommentResponse(Long id,
                              Long scenarioId,
                              Long parentId,
                              String content,
                              String mentions,
                              Long authorId,
                              String authorName,
                              LocalDateTime createdAt) {
}
