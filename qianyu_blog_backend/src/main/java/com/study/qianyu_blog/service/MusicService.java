package com.study.qianyu_blog.service;

import com.study.qianyu_blog.dto.MusicTrackResponse;
import com.study.qianyu_blog.entity.MusicTrack;
import com.study.qianyu_blog.exception.BusinessException;
import com.study.qianyu_blog.repository.MusicTrackRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class MusicService {
    private final MusicTrackRepository musicTrackRepository;
    private final Path musicStoragePath;

    public MusicService(MusicTrackRepository musicTrackRepository, @Value("${qianyu.storage.music-dir:uploads/music}") String musicDir) {
        this.musicTrackRepository = musicTrackRepository;
        this.musicStoragePath = Path.of(musicDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<MusicTrackResponse> list(String channel) {
        List<MusicTrack> tracks = (channel != null && !channel.isBlank())
                ? musicTrackRepository.findByChannelOrderByCreatedAtDesc(channel)
                : musicTrackRepository.findAllByOrderByCreatedAtDesc();
        return tracks.stream().map(this::toResponse).toList();
    }

    @Transactional
    public MusicTrackResponse upload(String title, String artist, String channel, MultipartFile audioFile, MultipartFile lyricFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(40000, "音频文件不能为空");
        }

        try {
            MusicTrack track = new MusicTrack();
            storeAudioFile(track, audioFile);
            track.setTitle(title != null && !title.isBlank() ? title.trim() : stripExtension(track.getOriginalFileName()));
            track.setArtist(normalizeOptionalText(artist));
            track.setChannel(channel != null && !channel.isBlank() ? channel : "calm");
            track.setLyricsContent(readLyricContent(lyricFile));
            return toResponse(musicTrackRepository.save(track));
        } catch (IOException exception) {
            throw new BusinessException(50000, "保存音频文件失败");
        }
    }

    @Transactional
    public MusicTrackResponse update(
            Long id,
            String title,
            String artist,
            String lyricsContent,
            Boolean clearLyrics,
            MultipartFile audioFile,
            MultipartFile lyricFile
    ) {
        MusicTrack track = getEntity(id);
        String oldStoredFileName = track.getStoredFileName();

        try {
            if (audioFile != null && !audioFile.isEmpty()) {
                storeAudioFile(track, audioFile);
            }

            if (title != null) {
                String normalizedTitle = title.trim();
                if (normalizedTitle.isEmpty()) {
                    throw new BusinessException(40000, "音乐标题不能为空");
                }
                track.setTitle(normalizedTitle);
            }

            if (artist != null) {
                track.setArtist(normalizeOptionalText(artist));
            }

            if (lyricFile != null && !lyricFile.isEmpty()) {
                track.setLyricsContent(readLyricContent(lyricFile));
            } else if (Boolean.TRUE.equals(clearLyrics)) {
                track.setLyricsContent(null);
            } else if (lyricsContent != null) {
                track.setLyricsContent(normalizeLyricsContent(lyricsContent));
            }

            MusicTrack saved = musicTrackRepository.save(track);
            if (audioFile != null && !audioFile.isEmpty() && oldStoredFileName != null && !oldStoredFileName.equals(saved.getStoredFileName())) {
                deleteStoredFile(oldStoredFileName);
            }
            return toResponse(saved);
        } catch (IOException exception) {
            throw new BusinessException(50000, "更新音乐文件失败");
        }
    }

    @Transactional
    public void delete(Long id) {
        MusicTrack track = getEntity(id);
        deleteStoredFile(track.getStoredFileName());
        musicTrackRepository.delete(track);
    }

    @Transactional(readOnly = true)
    public Resource getAudioResource(Long id) {
        MusicTrack track = getEntity(id);
        Path path = resolveStoragePath(track.getStoredFileName());
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new BusinessException(40400, "音频文件不存在");
        }
        return resource;
    }

    @Transactional(readOnly = true)
    public MusicTrack getEntity(Long id) {
        return musicTrackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40400, "音乐不存在"));
    }

    public MusicTrackResponse toResponse(MusicTrack track) {
        return new MusicTrackResponse(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getOriginalFileName(),
                track.getContentType(),
                track.getFileSize(),
                track.getDurationSeconds(),
                track.getLyricsContent(),
                "/api/public/music/" + track.getId() + "/file",
                track.getChannel(),
                track.getCreatedAt(),
                track.getUpdatedAt()
        );
    }

    private void storeAudioFile(MusicTrack track, MultipartFile audioFile) throws IOException {
        Files.createDirectories(musicStoragePath);

        String originalFileName = audioFile.getOriginalFilename() == null ? "music" : audioFile.getOriginalFilename();
        String storedFileName = generateStoredFileName(originalFileName);
        Path target = resolveStoragePath(storedFileName);
        Files.copy(audioFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        track.setOriginalFileName(originalFileName);
        track.setStoredFileName(storedFileName);
        track.setContentType(audioFile.getContentType() == null || audioFile.getContentType().isBlank()
                ? "audio/mpeg"
                : audioFile.getContentType());
        track.setFileSize(audioFile.getSize());
    }

    private String generateStoredFileName(String originalFileName) {
        String suffix = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            suffix = originalFileName.substring(dotIndex);
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + suffix;
    }

    private Path resolveStoragePath(String storedFileName) {
        Path target = musicStoragePath.resolve(storedFileName).normalize();
        if (!target.startsWith(musicStoragePath)) {
            throw new BusinessException(40000, "非法文件路径");
        }
        return target;
    }

    private void deleteStoredFile(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveStoragePath(storedFileName));
        } catch (IOException ignored) {
        }
    }

    private String readLyricContent(MultipartFile lyricFile) throws IOException {
        if (lyricFile == null || lyricFile.isEmpty()) {
            return null;
        }
        return new String(lyricFile.getBytes(), StandardCharsets.UTF_8);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeLyricsContent(String lyricsContent) {
        if (lyricsContent == null) {
            return null;
        }
        String normalized = lyricsContent.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
