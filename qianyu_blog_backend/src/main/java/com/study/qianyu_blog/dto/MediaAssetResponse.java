package com.study.qianyu_blog.dto;

import java.time.LocalDateTime;

public record MediaAssetResponse(
        Long id,
        String originalFileName,
        String contentType,
        Long fileSize,
        String displayName,
        String altText,
        String fileUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
