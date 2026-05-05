package com.study.qianyu_blog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class SiteSettings {
    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    private String siteName;

    @Column(length = 255)
    private String siteSubtitle;

    @Column(length = 120)
    private String heroBadge;

    @Column(nullable = false, length = 120)
    private String heroTitle;

    @Column(nullable = false, length = 500)
    private String heroDescription;

    @Column(length = 500)
    private String avatarImageUrl;

    @Column(length = 500)
    private String heroBackgroundImageUrl;

    @Column(length = 500)
    private String defaultPostCoverUrl;

    @Column(length = 500)
    private String githubUrl;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String footerText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
