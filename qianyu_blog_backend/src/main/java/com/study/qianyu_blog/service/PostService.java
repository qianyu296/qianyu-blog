package com.study.qianyu_blog.service;

import com.study.qianyu_blog.dto.PageResponse;
import com.study.qianyu_blog.dto.PostRequest;
import com.study.qianyu_blog.dto.PostResponse;
import com.study.qianyu_blog.entity.Category;
import com.study.qianyu_blog.entity.Post;
import com.study.qianyu_blog.enums.PostStatus;
import com.study.qianyu_blog.exception.BusinessException;
import com.study.qianyu_blog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    public PostService(PostRepository postRepository, CategoryService categoryService, TagService tagService) {
        this.postRepository = postRepository;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> publicList(Long categoryId, int page, int size) {
        Pageable pageable = pageable(page, size);
        Page<Post> posts;
        if (categoryId != null) {
            posts = postRepository.findByStatusAndCategoryIdOrderByPinnedAndCreatedAt(PostStatus.PUBLISHED, categoryId, pageable);
        } else {
            posts = postRepository.findByStatusExcludingCategoryName(PostStatus.PUBLISHED, "碎碎念", pageable);
        }
        return toPage(posts);
    }

    @Transactional(readOnly = true)
    public PostResponse publicDetail(Long id) {
        Post post = getEntity(id);
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new BusinessException(40400, "文章不存在");
        }
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> adminList(int page, int size) {
        return toPage(postRepository.findAll(pageable(page, size)));
    }

    @Transactional(readOnly = true)
    public PostResponse adminDetail(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public PostResponse create(PostRequest request) {
        Post post = new Post();
        apply(post, request);
        Post saved = postRepository.save(post);
        tagService.updateTagsForPost(saved, request.tags());
        return toResponse(saved);
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request) {
        Post post = getEntity(id);
        apply(post, request);
        Post saved = postRepository.save(post);
        tagService.updateTagsForPost(saved, request.tags());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new BusinessException(40400, "文章不存在");
        }
        postRepository.deleteById(id);
    }

    private Post getEntity(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new BusinessException(40400, "文章不存在"));
    }

    private void apply(Post post, PostRequest request) {
        Category category = categoryService.getEntity(request.categoryId());
        post.setTitle(request.title());
        post.setSummary(normalizeOptionalText(request.summary()));
        post.setContent(request.content());
        post.setCoverImageUrl(normalizeOptionalText(request.coverImageUrl()));
        post.setCategory(category);
        post.setStatus(request.status());
        post.setIsPinned(request.isPinned() != null && request.isPinned());
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<PostResponse> toPage(Page<Post> page) {
        return new PageResponse<>(page.getContent().stream().map(this::toResponse).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getContent(),
                post.getCoverImageUrl(),
                post.getStatus(),
                post.getIsPinned(),
                categoryService.toResponse(post.getCategory()),
                tagService.getTagsForPost(post.getId()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
