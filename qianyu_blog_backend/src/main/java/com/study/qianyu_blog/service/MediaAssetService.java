package com.study.qianyu_blog.service;

import com.study.qianyu_blog.dto.MediaAssetResponse;
import com.study.qianyu_blog.entity.MediaAsset;
import com.study.qianyu_blog.exception.BusinessException;
import com.study.qianyu_blog.repository.MediaAssetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class MediaAssetService {
    private final MediaAssetRepository mediaAssetRepository;
    private final Path mediaStoragePath;

    public MediaAssetService(MediaAssetRepository mediaAssetRepository, @Value("${qianyu.storage.media-dir:uploads/media}") String mediaDir) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaStoragePath = Path.of(mediaDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<MediaAssetResponse> list() {
        return mediaAssetRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MediaAssetResponse uploadImage(String displayName, String altText, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40000, "图片文件不能为空");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new BusinessException(40000, "仅支持上传图片文件");
        }

        try {
            Files.createDirectories(mediaStoragePath);

            String originalFileName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
            String storedFileName = generateStoredFileName(originalFileName);
            Path target = resolveStoragePath(storedFileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            MediaAsset asset = new MediaAsset();
            asset.setOriginalFileName(originalFileName);
            asset.setStoredFileName(storedFileName);
            asset.setContentType(contentType);
            asset.setFileSize(file.getSize());
            asset.setDisplayName(normalize(displayName));
            asset.setAltText(normalize(altText));
            return toResponse(mediaAssetRepository.save(asset));
        } catch (IOException exception) {
            throw new BusinessException(50000, "保存图片失败");
        }
    }

    @Transactional
    public void delete(Long id) {
        MediaAsset asset = getEntity(id);
        deleteStoredFile(asset.getStoredFileName());
        mediaAssetRepository.delete(asset);
    }

    @Transactional(readOnly = true)
    public Resource getFileResource(Long id) {
        MediaAsset asset = getEntity(id);
        Path path = resolveStoragePath(asset.getStoredFileName());
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new BusinessException(40400, "图片文件不存在");
        }
        return resource;
    }

    @Transactional(readOnly = true)
    public MediaAsset getEntity(Long id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40400, "图片资源不存在"));
    }

    public MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getOriginalFileName(),
                asset.getContentType(),
                asset.getFileSize(),
                asset.getDisplayName(),
                asset.getAltText(),
                "/api/public/media/" + asset.getId() + "/file",
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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
        Path target = mediaStoragePath.resolve(storedFileName).normalize();
        if (!target.startsWith(mediaStoragePath)) {
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
}
