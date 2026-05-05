package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.*;
import com.study.qianyu_blog.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicBlogController {
    private final CategoryService categoryService;
    private final PostService postService;
    private final TagService tagService;

    public PublicBlogController(CategoryService categoryService, PostService postService, TagService tagService) {
        this.categoryService = categoryService;
        this.postService = postService;
        this.tagService = tagService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.ok(categoryService.list());
    }

    @GetMapping("/tags")
    public ApiResponse<List<String>> tags() {
        return ApiResponse.ok(tagService.getAllDistinctTags());
    }

    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostResponse>> posts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(postService.publicList(categoryId, page, size));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<PostResponse> post(@PathVariable Long id) {
        return ApiResponse.ok(postService.publicDetail(id));
    }
}
