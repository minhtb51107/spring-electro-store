package com.minh.springelectrostore.product.service.impl;

import com.minh.springelectrostore.product.dto.request.CategoryRequest;
import com.minh.springelectrostore.product.dto.response.CategoryResponse;
import com.minh.springelectrostore.product.entity.Category;
import com.minh.springelectrostore.product.mapper.CategoryMapper;
import com.minh.springelectrostore.product.repository.CategoryRepository;
// TODO: (Module 2) Bỏ comment dòng này khi ProductRepository tồn tại
// import com.minh.springelectrostore.product.repository.ProductRepository;
import com.minh.springelectrostore.product.service.CategoryService;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.shared.util.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable; // <-- THÊM IMPORT
import org.springframework.cache.annotation.CacheEvict; // <-- THÊM IMPORT

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SlugService slugService;
    // TODO: (Module 2) Bỏ comment dòng này khi ProductRepository tồn tại
    // private final ProductRepository productRepository;

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        // 1. Kiểm tra tên trùng
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Tên danh mục '" + request.getName() + "' đã tồn tại.");
        }

        // 2. Map DTO -> Entity (chỉ map 'name')
        Category category = categoryMapper.toCategory(request);

        // 3. Xử lý logic 'parentId' (Phần quan trọng)
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha với ID: " + request.getParentId()));
            category.setParent(parent);
        }

        // 4. Tạo slug và lưu
        category.setSlug(slugService.toSlug(request.getName()));
        Category savedCategory = categoryRepository.save(category);
        log.info("Đã tạo danh mục mới: {}", savedCategory.getName());

        // 5. Map sang DTO Response
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("categories")
    public List<CategoryResponse> getAllCategoriesAsTree() {
        // 1. Chỉ tìm các danh mục gốc (parent_id IS NULL)
        List<Category> rootCategories = categoryRepository.findAllRootCategories();
        
        // 2. Dùng mapper để đệ quy
        // MapStruct (CategoryMapper) sẽ tự động gọi toCategoryResponseSet
        // cho trường 'children', tạo ra cấu trúc cây.
        return rootCategories.stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        // 1. Tìm danh mục
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        // 2. Kiểm tra tên trùng (nếu tên bị đổi)
        if (!existingCategory.getName().equals(request.getName())) {
            if (categoryRepository.findByName(request.getName()).isPresent()) {
                throw new BadRequestException("Tên danh mục '" + request.getName() + "' đã tồn tại.");
            }
            // 3. Cập nhật slug nếu tên đổi
            existingCategory.setSlug(slugService.toSlug(request.getName()));
        }

        // 4. Cập nhật logic 'parentId'
        if (request.getParentId() != null) {
            // 4a. Kiểm tra logic "vòng lặp": không thể tự làm cha của chính mình
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Không thể đặt danh mục làm cha của chính nó.");
            }
            // 4b. Tìm cha mới
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha với ID: " + request.getParentId()));
            existingCategory.setParent(parent);
        } else {
            // 4c. Nếu parentId gửi lên là null -> gỡ bỏ cha
            existingCategory.setParent(null);
        }

        // 5. Dùng MapStruct cập nhật trường 'name'
        categoryMapper.updateCategoryFromRequest(request, existingCategory);

        // 6. Lưu và trả về
        Category updatedCategory = categoryRepository.save(existingCategory);
        log.info("Đã cập nhật danh mục ID {}: {}", id, updatedCategory.getName());
        
        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        // 1. Kiểm tra ràng buộc nghiệp vụ: Không cho xóa nếu có danh mục con
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new BadRequestException("Không thể xóa danh mục này. Vui lòng xóa các danh mục con trước.");
        }

        // TODO: (Module 2) Kiểm tra ràng buộc nghiệp vụ: Không cho xóa nếu có sản phẩm
        // (Bỏ comment khi có ProductRepository)
        /*
        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Không thể xóa danh mục này. Vẫn còn sản phẩm liên kết.");
        }
        */

        // 2. Xóa
        categoryRepository.deleteById(id);
        log.info("Đã xóa danh mục ID: {}", id);
    }
}