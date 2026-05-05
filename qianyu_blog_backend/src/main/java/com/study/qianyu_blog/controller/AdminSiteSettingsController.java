package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.dto.SiteSettingsRequest;
import com.study.qianyu_blog.dto.SiteSettingsResponse;
import com.study.qianyu_blog.service.SiteSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/site-settings")
public class AdminSiteSettingsController {
    private final SiteSettingsService siteSettingsService;

    public AdminSiteSettingsController(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
    }

    @GetMapping
    public ApiResponse<SiteSettingsResponse> getSettings() {
        return ApiResponse.ok(siteSettingsService.getSettings());
    }

    @PostMapping
    public ApiResponse<SiteSettingsResponse> saveSettings(@Valid @RequestBody SiteSettingsRequest request) {
        return ApiResponse.ok(siteSettingsService.saveSettings(request));
    }
}
