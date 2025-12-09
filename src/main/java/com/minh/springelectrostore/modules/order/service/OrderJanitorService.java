package com.minh.springelectrostore.modules.order.service;

import com.minh.springelectrostore.modules.order.entity.Order;
import com.minh.springelectrostore.modules.order.entity.OrderItem;
import com.minh.springelectrostore.modules.order.entity.OrderStatus;
import com.minh.springelectrostore.modules.order.repository.OrderRepository;
import com.minh.springelectrostore.modules.product.service.InventoryService; // [QUAN TRỌNG] Dùng service thay vì Repository
import com.minh.springelectrostore.modules.promotion.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderJanitorService {

    private final OrderRepository orderRepository;
    
    // Thay thế ProductVariantRepository bằng InventoryService
    // Lý do: InventoryService có cơ chế Lock (khóa) để tránh lỗi khi cộng kho
    private final InventoryService inventoryService; 
    
    private final VoucherService voucherService;

    /**
     * Cron Job: Chạy mỗi 1 phút (60000ms).
     * Quét các đơn hàng PENDING quá 15 phút và hủy chúng.
     */
    @Scheduled(fixedRate = 60000) 
    @Transactional // Mở Transaction cho đợt quét này
    public void cancelUnpaidOrders() {
        // 1. Định nghĩa "Quá hạn": 15 phút trước
        OffsetDateTime timeoutThreshold = OffsetDateTime.now().minusMinutes(15);

        // 2. Tìm đơn hàng treo
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, 
                timeoutThreshold
        );

        if (!expiredOrders.isEmpty()) {
            log.info("[Janitor] Tìm thấy {} đơn hàng treo quá hạn. Bắt đầu dọn dẹp...", expiredOrders.size());
        }

        // 3. Xử lý từng đơn
        for (Order order : expiredOrders) {
            cancelAndRestoreStock(order);
        }
    }

    // Tách logic xử lý 1 đơn ra hàm riêng
    private void cancelAndRestoreStock(Order order) {
        try {
            log.info("-> Đang tự động hủy đơn hàng ID: {}", order.getId());

            // A. Cập nhật trạng thái
            order.setStatus(OrderStatus.CANCELLED);
            order.setNotes(order.getNotes() + " [Hủy tự động do quá hạn thanh toán]");
            // Lưu trạng thái trước
            orderRepository.save(order);

            // B. Hoàn trả tồn kho (QUAN TRỌNG: Dùng InventoryService)
            for (OrderItem item : order.getItems()) {
                // Gọi hàm restoreStock đã viết trong InventoryService
                inventoryService.restoreStock(
                        item.getProductVariant().getId(), 
                        item.getQuantity()
                );
            }
            
            // C. Hoàn trả Voucher
            voucherService.refundVoucher(order.getId());

            log.info("-> Đã hủy và hoàn kho thành công đơn hàng ID: {}", order.getId());

        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp đơn hàng ID: {}", order.getId(), e);
            // Không throw exception để vòng lặp tiếp tục chạy cho đơn khác
        }
    }
}