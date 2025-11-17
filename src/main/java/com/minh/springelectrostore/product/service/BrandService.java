package com.minh.springelectrostore.product.service;

import com.minh.springelectrostore.product.dto.request.BrandRequest;
import com.minh.springelectrostore.product.dto.response.BrandResponse;
import java.util.List;

public interface BrandService {

    /**
     * Tạo một thương hiệu mới.
     * Chỉ Admin mới có quyền này.
     * @param request DTO chứa thông tin thương hiệu mới.
     * @return DTO của thương hiệu đã được tạo.
     */
    BrandResponse createBrand(BrandRequest request);

    /**
     * Lấy thông tin một thương hiệu bằng ID.
     * @param id ID của thương hiệu.
     * @return DTO của thương hiệu.
     */
    BrandResponse getBrandById(Long id);

    /**
     * Lấy danh sách tất cả thương hiệu.
     * @return List DTO các thương hiệu.
     */
    List<BrandResponse> getAllBrands();

    /**
     * Cập nhật một thương hiệu đã tồn tại.
     * Chỉ Admin mới có quyền này.
     * @param id ID của thương hiệu cần cập nhật.
     * @param request DTO chứa thông tin cập nhật.
     * @return DTO của thương hiệu sau khi cập nhật.
     */
    BrandResponse updateBrand(Long id, BrandRequest request);

    /**
     * Xóa một thương hiệu.
     * Chỉ Admin mới có quyền này.
     * @param id ID của thương hiệu cần xóa.
     */
    void deleteBrand(Long id);
}