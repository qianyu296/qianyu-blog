package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.dto.PageResponse;
import com.study.qianyu_blog.dto.PostRequest;
import com.study.qianyu_blog.dto.PostResponse;
import com.study.qianyu_blog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {
    private final PostService postService;

    public AdminPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(postService.adminList(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(postService.adminDetail(id));
    }

    @PostMapping
    public ApiResponse<PostResponse> create(@Valid @RequestBody PostRequest request) {
        return ApiResponse.ok(postService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PostResponse> update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return ApiResponse.ok(postService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ApiResponse.ok(null);
    }
}
