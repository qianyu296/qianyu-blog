package com.study.qianyu_blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteSettingsRequest(
        @NotBlank @Size(max = 120) String siteName,
        @Size(max = 255) String siteSubtitle,
        @Size(max = 120) String heroBadge,
        @NotBlank @Size(max = 120) String heroTitle,
        @NotBlank @Size(max = 500) String heroDescription,
        @Size(max = 500) String avatarImageUrl,
        @Size(max = 500) String heroBackgroundImageUrl,
        @Size(max = 500) String defaultPostCoverUrl,
        @Size(max = 500) String githubUrl,
        @Size(max = 255) String email,
        @Size(max = 255) String footerText
) {
}
