package com.study.qianyu_blog.dto;

import com.study.qianyu_blog.enums.PostStatus;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String summary,
        String content,
        PostStatus status,
        CategoryResponse category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
