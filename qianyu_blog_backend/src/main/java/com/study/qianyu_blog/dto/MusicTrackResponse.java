package com.study.qianyu_blog.dto;

import java.time.LocalDateTime;

public record MusicTrackResponse(
        Long id,
        String title,
        String artist,
        String originalFileName,
        String contentType,
        Long fileSize,
        Integer durationSeconds,
        String lyricsContent,
        String fileUrl,
        String channel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
