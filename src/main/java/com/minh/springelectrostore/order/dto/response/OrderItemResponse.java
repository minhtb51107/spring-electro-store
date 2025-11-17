package com.minh.springelectrostore.order.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO này chứa thông tin chi tiết của MỘT món hàng trong đơn hàng.
 * Nó thực hiện "snapshot" dữ liệu.
 */
@Data
@Builder
public class OrderItemResponse {
    
    // Thông tin về sản phẩm đã mua
    private Long productVariantId;
    private String sku;
    private String productName; // Snapshot tên sản phẩm
    private String thumbnailUrl; // Snapshot ảnh

    // Thông tin về giao dịch
    private int quantity;
    private BigDecimal priceAtPurchase; // <-- Dữ liệu snapshot quan trọng!
    private BigDecimal lineTotal; // (quantity * priceAtPurchase)
}