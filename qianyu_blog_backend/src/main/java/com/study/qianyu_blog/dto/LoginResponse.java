package com.study.qianyu_blog.dto;

public record LoginResponse(String token, String tokenType, long expiresInMinutes) {
}
