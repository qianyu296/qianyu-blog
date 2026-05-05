package com.study.qianyu_blog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_settings")
@Getter
@Setter
public class AiSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key", nullable = false)
    private String chatApiKey;

    @Column
    private String imageApiKey;

    @Column(nullable = false)
    private String apiEndpoint;

    private String imageApiEndpoint;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String imageModelName = "dall-e-3";

    private Boolean enabled = true;

    @Column(nullable = false)
    private Integer maxTokens = 4096;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
