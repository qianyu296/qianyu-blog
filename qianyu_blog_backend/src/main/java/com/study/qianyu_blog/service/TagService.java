package com.study.qianyu_blog.service;

import com.study.qianyu_blog.entity.Post;
import com.study.qianyu_blog.entity.Tag;
import com.study.qianyu_blog.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {
    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public void updateTagsForPost(Post post, List<String> tagNames) {
        tagRepository.deleteByPostId(post.getId());
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        for (String name : tagNames) {
            if (name == null || name.isBlank()) continue;
            Tag tag = new Tag();
            tag.setPost(post);
            tag.setName(name.trim());
            tag.setSlug(name.trim().toLowerCase().replaceAll("\\s+", "-"));
            tagRepository.save(tag);
        }
    }

    public List<String> getTagsForPost(Long postId) {
        return tagRepository.findByPostId(postId).stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
    }

    public List<String> getAllDistinctTags() {
        return tagRepository.findAllDistinctNames();
    }
}
