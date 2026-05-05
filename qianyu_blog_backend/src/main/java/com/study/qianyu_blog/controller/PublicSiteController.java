package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.dto.SiteSettingsResponse;
import com.study.qianyu_blog.service.SiteSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/site")
public class PublicSiteController {
    private final SiteSettingsService siteSettingsService;

    public PublicSiteController(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
    }

    @GetMapping("/settings")
    public ApiResponse<SiteSettingsResponse> settings() {
        return ApiResponse.ok(siteSettingsService.getSettings());
    }
}
