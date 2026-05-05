package com.study.qianyu_blog.repository;

import com.study.qianyu_blog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByPostId(Long postId);

    @Modifying
    @Query("DELETE FROM Tag t WHERE t.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Query("SELECT DISTINCT t.name FROM Tag t ORDER BY t.name")
    List<String> findAllDistinctNames();
}
