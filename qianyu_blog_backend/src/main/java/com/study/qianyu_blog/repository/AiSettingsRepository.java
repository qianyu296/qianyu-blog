package com.study.qianyu_blog.repository;

import com.study.qianyu_blog.entity.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiSettingsRepository extends JpaRepository<AiSettings, Long> {
    Optional<AiSettings> findTopByEnabledTrue();
}
