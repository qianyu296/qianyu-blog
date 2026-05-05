package com.study.qianyu_blog.repository;

import com.study.qianyu_blog.entity.Post;
import com.study.qianyu_blog.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    Page<Post> findByStatusAndCategoryId(PostStatus status, Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = :status ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<Post> findByStatusOrderByPinnedAndCreatedAt(@Param("status") PostStatus status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = :status AND p.category.id = :categoryId ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<Post> findByStatusAndCategoryIdOrderByPinnedAndCreatedAt(@Param("status") PostStatus status, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = :status AND p.category.name <> :excludeName ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<Post> findByStatusExcludingCategoryName(@Param("status") PostStatus status, @Param("excludeName") String excludeName, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}
