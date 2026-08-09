package com.sis.iids.collab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * R-15 评论请求（FR-04-02）。
 */
public record CommentRequest(@NotBlank @Size(max = 2000) String content,
                             Long parentId) {
}
