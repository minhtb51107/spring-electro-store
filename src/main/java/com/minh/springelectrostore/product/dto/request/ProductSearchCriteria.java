package com.minh.springelectrostore.product.dto.request;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO này không dùng để validate, mà dùng để gom nhóm 
 * các tham số tìm kiếm (query params) từ URL.
 */
@Data
public class ProductSearchCriteria {
    // Tìm theo tên
    private String name;

    // Tìm theo Category (dùng slug cho thân thiện)
    private String categorySlug;
    
    // Tìm theo Brand (dùng slug)
    private String brandSlug;

    // Lọc theo khoảng giá
    private BigDecimal priceGte; // "gte" = Greater Than or Equal
    private BigDecimal priceLte; // "lte" = Less Than or Equal
    
    // (Trong tương lai, bạn có thể dễ dàng thêm: private String color; private String ram; ...)
}