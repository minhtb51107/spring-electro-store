package com.minh.springelectrostore.order.service;

import com.minh.springelectrostore.order.entity.Order;
import com.minh.springelectrostore.order.entity.OrderItem;
import com.minh.springelectrostore.order.entity.OrderStatus;
import com.minh.springelectrostore.order.repository.OrderRepository;
import com.minh.springelectrostore.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.promotion.service.VoucherService; // Dùng để hoàn voucher
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
    private final ProductVariantRepository productVariantRepository;
    private final VoucherService voucherService;

    /**
     * Cron Job: Chạy mỗi 1 phút (60000ms).
     * Quét các đơn hàng PENDING quá 15 phút và hủy chúng.
     */
    @Scheduled(fixedRate = 60000) 
    @Transactional // Mở Transaction cho đợt quét này
    public void cancelUnpaidOrders() {
        // 1. Định nghĩa "Quá hạn": Là những đơn tạo trước 15 phút tính từ bây giờ
        OffsetDateTime timeoutThreshold = OffsetDateTime.now().minusMinutes(15);

        // 2. Tìm đơn hàng treo
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, 
                timeoutThreshold
        );

        if (!expiredOrders.isEmpty()) {
            log.info("Tìm thấy {} đơn hàng treo quá hạn. Bắt đầu dọn dẹp...", expiredOrders.size());
        }

        // 3. Xử lý từng đơn
        for (Order order : expiredOrders) {
            cancelAndRestoreStock(order);
        }
    }

    // Tách logic xử lý 1 đơn ra hàm riêng
    private void cancelAndRestoreStock(Order order) {
        try {
            log.info("Đang tự động hủy đơn hàng ID: {}", order.getId());

            // A. Cập nhật trạng thái
            order.setStatus(OrderStatus.CANCELLED);
            order.setNotes(order.getNotes() + " [Hủy tự động do quá hạn thanh toán]");
            orderRepository.save(order);

            // B. Hoàn trả tồn kho cho từng món hàng
            for (OrderItem item : order.getItems()) {
                productVariantRepository.increaseStock(
                        item.getProductVariant().getId(), 
                        item.getQuantity()
                );
            }
            
            // C. Hoàn trả Voucher (nếu có dùng) - Module 11
            voucherService.refundVoucher(order.getId());

            log.info("-> Đã hủy và hoàn kho thành công đơn hàng ID: {}", order.getId());

        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp đơn hàng ID: {}", order.getId(), e);
            // Không throw exception ở đây để vòng lặp tiếp tục chạy cho các đơn khác
        }
    }
}