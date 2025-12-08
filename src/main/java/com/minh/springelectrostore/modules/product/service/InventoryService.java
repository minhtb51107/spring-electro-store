package com.minh.springelectrostore.modules.product.service;

/**
 * Interface chịu trách nhiệm quản lý kho hàng (Inventory).
 * Giúp tách biệt logic Kho ra khỏi logic Đơn hàng (Order).
 */
public interface InventoryService {

    /**
     * Giữ hàng (Trừ kho) an toàn với Locking.
     * @param variantId ID biến thể sản phẩm
     * @param quantity Số lượng cần mua
     * @throws com.minh.springelectrostore.common.exception.BadRequestException nếu hết hàng hoặc lỗi lock.
     */
    void reserveStock(Long variantId, Integer quantity);
}