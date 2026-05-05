package com.study.qianyu_blog.dto;

import com.study.qianyu_blog.enums.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 255) String summary,
        @NotBlank String content,
        @Size(max = 500) String coverImageUrl,
        @NotNull Long categoryId,
        List<String> tags,
        Boolean isPinned,
        @NotNull PostStatus status
) {
}
