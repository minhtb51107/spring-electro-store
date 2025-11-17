package com.minh.springelectrostore.order.service.impl;

import com.minh.springelectrostore.notification.service.NotificationService;
import com.minh.springelectrostore.order.dto.request.AdminOrderSearchCriteria;
import com.minh.springelectrostore.order.dto.request.UpdateOrderStatusRequest;
import com.minh.springelectrostore.order.dto.response.OrderResponse;
import com.minh.springelectrostore.order.entity.Order;
import com.minh.springelectrostore.order.entity.OrderStatus;
import com.minh.springelectrostore.order.mapper.OrderMapper;
import com.minh.springelectrostore.order.repository.AdminOrderSpecification;
import com.minh.springelectrostore.order.repository.OrderRepository;
import com.minh.springelectrostore.order.service.AdminOrderService;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.shared.service.EmailService; // Sẽ dùng để thông báo cho khách
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("adminOrderService") // Đặt tên bean rõ ràng
@RequiredArgsConstructor
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final EmailService emailService;
    private final NotificationService notificationService;
    // (Chúng ta không cần ProductVariantRepository ở đây nếu chỉ cập nhật status)

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrders(AdminOrderSearchCriteria criteria, Pageable pageable) {
        // 1. Tạo Specification từ criteria
        Specification<Order> spec = new AdminOrderSpecification(criteria);

        // 2. Gọi hàm findAll(spec, pageable)
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        
        // 3. Map sang DTO (dùng DTO chi tiết vì Admin cần xem)
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetailById(Long orderId) {
        // Dùng hàm JOIN FETCH để lấy kèm items
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));
        
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId) // Không cần JOIN FETCH vì chỉ cập nhật status
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = request.getNewStatus();

        // Logic nghiệp vụ: Ngăn chặn các thay đổi trạng thái không hợp lệ
        if (oldStatus == OrderStatus.DELIVERED || oldStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Không thể cập nhật trạng thái cho đơn hàng đã '" + oldStatus + "'.");
        }
        
        if (oldStatus == newStatus) {
            return orderMapper.toOrderResponse(order); // Không có gì thay đổi
        }

        // Cập nhật trạng thái
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        log.info("Admin đã cập nhật đơn hàng ID: {} từ {} -> {}", 
                 savedOrder.getId(), oldStatus, newStatus);

        // Kỹ thuật "Pro": Gửi email cho khách hàng khi trạng thái thay đổi
        if (newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.SHIPPED) {
            String subject = String.format("Cập nhật đơn hàng #%d: %s", savedOrder.getId(), newStatus);
            String body = String.format("<p>Đơn hàng #%d của bạn đã được cập nhật trạng thái thành: <strong>%s</strong>.</p>",
                                        savedOrder.getId(), newStatus);
            
            // Lấy email khách hàng (cần JOIN FETCH customer và user,
            // nhưng để đơn giản, ta làm 1 query riêng)
            String customerEmail = order.getCustomer().getUser().getEmail(); 
            String message = String.format("Đơn hàng #%d của bạn đã chuyển sang trạng thái: %s", 
                                           savedOrder.getId(), newStatus);
            
            notificationService.sendNotificationToUser(customerEmail, message);
            
            // Gửi bất đồng bộ
            emailService.sendEmail(customerEmail, subject, body);
        }

        return orderMapper.toOrderResponse(savedOrder);
    }
}