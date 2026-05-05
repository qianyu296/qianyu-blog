package com.study.qianyu_blog.service;

import com.study.qianyu_blog.dto.SiteSettingsRequest;
import com.study.qianyu_blog.dto.SiteSettingsResponse;
import com.study.qianyu_blog.entity.SiteSettings;
import com.study.qianyu_blog.repository.SiteSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteSettingsService {
    private static final long SETTINGS_ID = 1L;

    private final SiteSettingsRepository siteSettingsRepository;

    public SiteSettingsService(SiteSettingsRepository siteSettingsRepository) {
        this.siteSettingsRepository = siteSettingsRepository;
    }

    @Transactional(readOnly = true)
    public SiteSettingsResponse getSettings() {
        return toResponse(siteSettingsRepository.findById(SETTINGS_ID).orElseGet(this::defaultSettings));
    }

    @Transactional
    public SiteSettingsResponse saveSettings(SiteSettingsRequest request) {
        SiteSettings settings = siteSettingsRepository.findById(SETTINGS_ID).orElseGet(this::defaultSettings);
        settings.setId(SETTINGS_ID);
        settings.setSiteName(request.siteName().trim());
        settings.setSiteSubtitle(normalize(request.siteSubtitle()));
        settings.setHeroBadge(normalize(request.heroBadge()));
        settings.setHeroTitle(request.heroTitle().trim());
        settings.setHeroDescription(request.heroDescription().trim());
        settings.setAvatarImageUrl(normalize(request.avatarImageUrl()));
        settings.setHeroBackgroundImageUrl(normalize(request.heroBackgroundImageUrl()));
        settings.setDefaultPostCoverUrl(normalize(request.defaultPostCoverUrl()));
        settings.setGithubUrl(normalize(request.githubUrl()));
        settings.setEmail(normalize(request.email()));
        settings.setFooterText(normalize(request.footerText()));
        return toResponse(siteSettingsRepository.save(settings));
    }

    public SiteSettingsResponse toResponse(SiteSettings settings) {
        return new SiteSettingsResponse(
                settings.getId(),
                settings.getSiteName(),
                settings.getSiteSubtitle(),
                settings.getHeroBadge(),
                settings.getHeroTitle(),
                settings.getHeroDescription(),
                settings.getAvatarImageUrl(),
                settings.getHeroBackgroundImageUrl(),
                settings.getDefaultPostCoverUrl(),
                settings.getGithubUrl(),
                settings.getEmail(),
                settings.getFooterText(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }

    private SiteSettings defaultSettings() {
        SiteSettings settings = new SiteSettings();
        settings.setId(SETTINGS_ID);
        settings.setSiteName("千语博客");
        settings.setSiteSubtitle("记录学习、开发和生活片段");
        settings.setHeroBadge("技术 · 生活 · 思考");
        settings.setHeroTitle("千语博客");
        settings.setHeroDescription("记录学习、开发和生活片段，在代码与文字间寻找平衡");
        settings.setFooterText("用代码和文字持续构建自己的内容站。");
        return settings;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
