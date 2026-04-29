package com.study.qianyu_blog.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
