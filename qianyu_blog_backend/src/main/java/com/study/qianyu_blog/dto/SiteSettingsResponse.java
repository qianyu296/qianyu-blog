package com.study.qianyu_blog.dto;

import java.time.LocalDateTime;

public record SiteSettingsResponse(
        Long id,
        String siteName,
        String siteSubtitle,
        String heroBadge,
        String heroTitle,
        String heroDescription,
        String avatarImageUrl,
        String heroBackgroundImageUrl,
        String defaultPostCoverUrl,
        String githubUrl,
        String email,
        String footerText,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
