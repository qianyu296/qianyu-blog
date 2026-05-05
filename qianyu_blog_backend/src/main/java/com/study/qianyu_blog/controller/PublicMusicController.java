package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.dto.MusicTrackResponse;
import com.study.qianyu_blog.entity.MusicTrack;
import com.study.qianyu_blog.service.MusicService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/public/music")
public class PublicMusicController {
    private final MusicService musicService;

    public PublicMusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping
    public ApiResponse<List<MusicTrackResponse>> list(@RequestParam(required = false) String channel) {
        return ApiResponse.ok(musicService.list(channel));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        MusicTrack track = musicService.getEntity(id);
        Resource resource = musicService.getAudioResource(id);
        String encodedName = URLEncoder.encode(track.getOriginalFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(track.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(resource);
    }
}
