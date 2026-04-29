package com.study.qianyu_blog.repository;

import com.study.qianyu_blog.entity.Post;
import com.study.qianyu_blog.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    Page<Post> findByStatusAndCategoryId(PostStatus status, Long categoryId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}
