package com.minh.springelectrostore.search.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Sự kiện này được "bắn" ra khi có thay đổi ở Product (PostgreSQL).
 * Nó chứa ID của sản phẩm cần đồng bộ.
 */
@Getter
public class ProductSyncEvent extends ApplicationEvent {

    private final Long productId;
    private final SyncAction action;

    public enum SyncAction {
        CREATE_UPDATE, // Dùng chung cho cả tạo mới và cập nhật
        DELETE         // Dùng cho xóa (nếu bạn dùng xóa cứng)
    }

    /**
     * Tạo một sự kiện đồng bộ sản phẩm mới.
     * @param source Nguồn phát sinh sự kiện (thường là 'this' từ Service)
     * @param productId ID của sản phẩm trong PostgreSQL
     * @param action Hành động (tạo/sửa hay xóa)
     */
    public ProductSyncEvent(Object source, Long productId, SyncAction action) {
        super(source);
        this.productId = productId;
        this.action = action;
    }
}