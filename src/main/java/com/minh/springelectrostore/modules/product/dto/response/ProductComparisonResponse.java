package com.minh.springelectrostore.modules.product.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductComparisonResponse {
    // Danh sách tên sản phẩm, ảnh, giá để hiển thị header cột
    private List<ProductSummary> products;
    
    // Map chứa thông số: Key = Tên thông số (Màn hình), Value = List giá trị tương ứng từng sản phẩm
    // Ví dụ: "Màn hình" -> ["6.1 inch", "6.7 inch"]
    private Map<String, List<String>> attributes;

    @Data
    @Builder
    public static class ProductSummary {
        private Long id;
        private String name;
        private BigDecimal price;
        private String thumbnailUrl;
        private String slug;
    }
}