package com.minh.springelectrostore.modules.product.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.common.exception.ResourceNotFoundException;
import com.minh.springelectrostore.common.util.SlugService;
import com.minh.springelectrostore.modules.product.dto.request.CategoryRequest;
import com.minh.springelectrostore.modules.product.dto.response.CategoryResponse;
import com.minh.springelectrostore.modules.product.entity.Category;
import com.minh.springelectrostore.modules.product.mapper.CategoryMapper;
import com.minh.springelectrostore.modules.product.repository.CategoryRepository;
import com.minh.springelectrostore.modules.product.service.CategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SlugService slugService;
    // private final ProductRepository productRepository; // Bỏ comment khi đã có module Product

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true) // Xóa sạch cache khi tạo mới
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Tên danh mục '" + request.getName() + "' đã tồn tại.");
        }

        Category category = categoryMapper.toCategory(request);

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha với ID: " + request.getParentId()));
            category.setParent(parent);
        }

        category.setSlug(slugService.toSlug(request.getName()));
        Category savedCategory = categoryRepository.save(category);
        log.info("Đã tạo danh mục mới: {}", savedCategory.getName());

        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'") // Lưu kết quả vào Redis với key 'categories::all'
    public List<CategoryResponse> getAllCategoriesAsTree() {
        // Hàm này query DB nặng (nếu nhiều danh mục), nên rất cần Cache
        List<Category> rootCategories = categoryRepository.findAllRootCategories();
        return rootCategories.stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true) // Xóa sạch cache khi cập nhật
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        if (!existingCategory.getName().equals(request.getName())) {
            if (categoryRepository.findByName(request.getName()).isPresent()) {
                throw new BadRequestException("Tên danh mục '" + request.getName() + "' đã tồn tại.");
            }
            existingCategory.setSlug(slugService.toSlug(request.getName()));
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Không thể đặt danh mục làm cha của chính nó.");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha với ID: " + request.getParentId()));
            existingCategory.setParent(parent);
        } else {
            existingCategory.setParent(null);
        }

        categoryMapper.updateCategoryFromRequest(request, existingCategory);
        Category updatedCategory = categoryRepository.save(existingCategory);
        log.info("Đã cập nhật danh mục ID {}: {}", id, updatedCategory.getName());
        
        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true) // Xóa sạch cache khi xóa
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new BadRequestException("Không thể xóa danh mục này. Vui lòng xóa các danh mục con trước.");
        }

        /* // Bỏ comment khi có ProductRepository
        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Không thể xóa danh mục này. Vẫn còn sản phẩm liên kết.");
        }
        */

        categoryRepository.deleteById(id);
        log.info("Đã xóa danh mục ID: {}", id);
    }
}