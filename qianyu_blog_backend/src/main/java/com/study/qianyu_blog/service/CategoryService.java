package com.study.qianyu_blog.service;

import com.study.qianyu_blog.dto.CategoryRequest;
import com.study.qianyu_blog.dto.CategoryResponse;
import com.study.qianyu_blog.entity.Category;
import com.study.qianyu_blog.exception.BusinessException;
import com.study.qianyu_blog.repository.CategoryRepository;
import com.study.qianyu_blog.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public CategoryService(CategoryRepository categoryRepository, PostRepository postRepository) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public Category getEntity(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new BusinessException(40400, "分类不存在"));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        validateUnique(request.name(), request.slug(), null);
        Category category = new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        validateUnique(request.name(), request.slug(), id);
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(40400, "分类不存在");
        }
        if (postRepository.existsByCategoryId(id)) {
            throw new BusinessException(40900, "分类下存在文章，不能删除");
        }
        categoryRepository.deleteById(id);
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription(), category.getCreatedAt(), category.getUpdatedAt());
    }

    private void validateUnique(String name, String slug, Long currentId) {
        categoryRepository.findAll().stream()
                .filter(category -> currentId == null || !category.getId().equals(currentId))
                .filter(category -> category.getName().equals(name) || category.getSlug().equals(slug))
                .findAny()
                .ifPresent(category -> {
                    throw new BusinessException(40900, "分类名称或标识已存在");
                });
    }
}
