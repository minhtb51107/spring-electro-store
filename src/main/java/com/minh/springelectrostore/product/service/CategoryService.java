package com.minh.springelectrostore.product.service;

import com.minh.springelectrostore.product.dto.request.CategoryRequest;
import com.minh.springelectrostore.product.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {

    /**
     * Tạo danh mục mới.
     * Chỉ Admin có quyền.
     * @param request DTO chứa name và parentId (nếu có).
     * @return DTO của danh mục đã tạo.
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Lấy danh sách TẤT CẢ danh mục dưới dạng CÂY (nested).
     * Chỉ trả về các danh mục gốc, các danh mục con sẽ nằm bên trong.
     * @return List DTO các danh mục gốc.
     */
    List<CategoryResponse> getAllCategoriesAsTree();

    /**
     * Cập nhật danh mục.
     * Chỉ Admin có quyền.
     * @param id ID của danh mục cần cập nhật.
     * @param request DTO chứa thông tin mới.
     * @return DTO của danh mục đã cập nhật.
     */
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /**
     * Xóa danh mục.
     * Chỉ Admin có quyền.
     * @param id ID của danh mục cần xóa.
     */
    void deleteCategory(Long id);
}