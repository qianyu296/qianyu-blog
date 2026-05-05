package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.dto.MusicTrackResponse;
import com.study.qianyu_blog.service.MusicService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/music")
public class AdminMusicController {
    private final MusicService musicService;

    public AdminMusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @PostMapping
    public ApiResponse<MusicTrackResponse> upload(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false, defaultValue = "calm") String channel,
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(required = false) MultipartFile lyricFile
    ) {
        return ApiResponse.ok(musicService.upload(title, artist, channel, audioFile, lyricFile));
    }

    @PutMapping("/{id}")
    public ApiResponse<MusicTrackResponse> update(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String lyricsContent,
            @RequestParam(required = false) Boolean clearLyrics,
            @RequestParam(required = false) MultipartFile audioFile,
            @RequestParam(required = false) MultipartFile lyricFile
    ) {
        return ApiResponse.ok(musicService.update(id, title, artist, lyricsContent, clearLyrics, audioFile, lyricFile));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        musicService.delete(id);
        return ApiResponse.ok(null);
    }
}
