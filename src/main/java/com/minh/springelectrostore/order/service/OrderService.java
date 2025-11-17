package com.minh.springelectrostore.order.service;

import com.minh.springelectrostore.order.dto.request.CheckoutRequest;
import com.minh.springelectrostore.order.dto.response.OrderResponse;
import com.minh.springelectrostore.order.dto.response.OrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    /**
     * Hàm quan trọng nhất: Xử lý checkout.
     * Được bọc trong @Transactional.
     * @param userEmail Email của khách hàng (lấy từ Authentication).
     * @param request DTO chứa thông tin giao hàng (địa chỉ, SĐT...).
     * @return DTO chi tiết của đơn hàng vừa tạo.
     */
    OrderResponse createOrderFromCart(String userEmail, CheckoutRequest request);

    /**
     * Lấy danh sách đơn hàng (tóm tắt) của khách hàng.
     * Dùng cho trang "Lịch sử đơn hàng".
     * @param userEmail Email của khách hàng.
     * @param pageable Thông tin phân trang.
     * @return Trang (Page) chứa các đơn hàng tóm tắt.
     */
    Page<OrderSummaryResponse> getMyOrders(String userEmail, Pageable pageable);

    /**
     * Lấy chi tiết 1 đơn hàng của khách hàng.
     * @param userEmail Email của khách hàng.
     * @param orderId ID của đơn hàng cần xem.
     * @return DTO chi tiết của đơn hàng.
     */
    OrderResponse getMyOrderDetail(String userEmail, Long orderId);

    /**
     * Khách hàng tự hủy đơn hàng (nếu status là PENDING).
     * @param userEmail Email của khách hàng.
     * @param orderId ID của đơn hàng cần hủy.
     * @return DTO chi tiết của đơn hàng sau khi hủy.
     */
    OrderResponse cancelMyOrder(String userEmail, Long orderId);
    
    // (Các API cho Admin (cập nhật status, lấy tất cả đơn...) 
    //  sẽ được thêm trong Module 5: Admin Dashboard)
}