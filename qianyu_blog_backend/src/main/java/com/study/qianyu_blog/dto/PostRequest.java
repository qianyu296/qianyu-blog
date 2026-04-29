package com.study.qianyu_blog.dto;

import com.study.qianyu_blog.enums.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 255) String summary,
        @NotBlank String content,
        @NotNull Long categoryId,
        @NotNull PostStatus status
) {
}
