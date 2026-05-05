package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.dto.MediaAssetResponse;
import com.study.qianyu_blog.service.MediaAssetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {
    private final MediaAssetService mediaAssetService;

    public AdminMediaController(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @GetMapping
    public ApiResponse<List<MediaAssetResponse>> list() {
        return ApiResponse.ok(mediaAssetService.list());
    }

    @PostMapping
    public ApiResponse<MediaAssetResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String altText
    ) {
        return ApiResponse.ok(mediaAssetService.uploadImage(displayName, altText, file));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        mediaAssetService.delete(id);
        return ApiResponse.ok(null);
    }
}
