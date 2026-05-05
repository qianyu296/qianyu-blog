package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.AiSettingsRequest;
import com.study.qianyu_blog.dto.AiPolishRequest;
import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.entity.AiSettings;
import com.study.qianyu_blog.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
public class AdminAiController {
    private final AiService aiService;

    public AdminAiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/settings")
    public ApiResponse<AiSettings> getSettings() {
        AiSettings settings = aiService.getSettings();
        if (settings != null) {
            settings.setChatApiKey(maskApiKey(settings.getChatApiKey()));
            settings.setImageApiKey(maskApiKey(settings.getImageApiKey()));
        }
        return ApiResponse.ok(settings);
    }

    @PostMapping("/settings")
    public ApiResponse<AiSettings> saveSettings(@RequestBody AiSettingsRequest request) {
        AiSettings existing = aiService.getSettings();
        AiSettings settings = new AiSettings();
        settings.setChatApiKey(request.getChatApiKey() != null && !request.getChatApiKey().isEmpty()
                ? request.getChatApiKey()
                : (existing != null ? existing.getChatApiKey() : ""));
        settings.setImageApiKey(request.getImageApiKey() != null && !request.getImageApiKey().isEmpty()
                ? request.getImageApiKey()
                : (existing != null ? existing.getImageApiKey() : settings.getChatApiKey()));
        settings.setApiEndpoint(request.getApiEndpoint());
        settings.setImageApiEndpoint(request.getImageApiEndpoint() != null && !request.getImageApiEndpoint().isEmpty()
                ? request.getImageApiEndpoint()
                : null);
        settings.setModelName(request.getModelName());
        settings.setImageModelName(request.getImageModelName());
        settings.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        settings.setMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
        return ApiResponse.ok(aiService.saveSettings(settings));
    }

    @PostMapping("/polish")
    public ApiResponse<Map<String, String>> polish(@RequestBody AiPolishRequest request) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            return ApiResponse.error(40003, "文章内容不能为空");
        }

        String polished = aiService.polishPost(request.getTitle(), request.getSummary(), request.getContent());
        return ApiResponse.ok(Map.of("content", polished));
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
