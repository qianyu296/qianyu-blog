package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.dto.CategoryResponse;
import com.study.qianyu_blog.dto.PageResponse;
import com.study.qianyu_blog.dto.PostResponse;
import com.study.qianyu_blog.service.CategoryService;
import com.study.qianyu_blog.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicBlogController {
    private final CategoryService categoryService;
    private final PostService postService;

    public PublicBlogController(CategoryService categoryService, PostService postService) {
        this.categoryService = categoryService;
        this.postService = postService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.ok(categoryService.list());
    }

    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostResponse>> posts(
            @RequestParam(required = false) Long categoryId,
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
