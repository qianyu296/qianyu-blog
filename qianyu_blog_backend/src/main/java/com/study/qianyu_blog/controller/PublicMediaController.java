package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.dto.MediaAssetResponse;
import com.study.qianyu_blog.entity.MediaAsset;
import com.study.qianyu_blog.service.MediaAssetService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/public/media")
public class PublicMediaController {
    private final MediaAssetService mediaAssetService;

    public PublicMediaController(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @GetMapping
    public ApiResponse<List<MediaAssetResponse>> list() {
        return ApiResponse.ok(mediaAssetService.list());
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        MediaAsset asset = mediaAssetService.getEntity(id);
        Resource resource = mediaAssetService.getFileResource(id);
        String encodedName = URLEncoder.encode(asset.getOriginalFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .body(resource);
    }
}
