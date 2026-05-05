package com.study.qianyu_blog.dto;

import com.study.qianyu_blog.enums.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String title,
        String summary,
        String content,
        String coverImageUrl,
        PostStatus status,
        Boolean isPinned,
        CategoryResponse category,
        List<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
