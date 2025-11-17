package com.minh.springelectrostore.order.controller;

import com.minh.springelectrostore.order.dto.request.CheckoutRequest;
import com.minh.springelectrostore.order.dto.response.OrderResponse;
import com.minh.springelectrostore.order.dto.response.OrderSummaryResponse;
import com.minh.springelectrostore.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // <-- BẢO MẬT: Mọi API đều yêu cầu đăng nhập
public class OrderController {

    private final OrderService orderService;

    /**
     * API QUAN TRỌNG NHẤT: XỬ LÝ CHECKOUT
     * Endpoint này sẽ kích hoạt logic @Transactional trong Service.
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request) {
        
        String userEmail = authentication.getName();
        // Gọi hàm "thần kỳ" @Transactional
        OrderResponse createdOrder = orderService.createOrderFromCart(userEmail, request);
        
        // Trả về 201 Created cùng với chi tiết đơn hàng
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    /**
     * API LẤY LỊCH SỬ ĐƠN HÀNG (Dạng danh sách tóm tắt)
     * Dùng cho trang "Tài khoản của tôi -> Đơn hàng của tôi".
     */
    @GetMapping("/my-history")
    public ResponseEntity<Page<OrderSummaryResponse>> getMyOrderHistory(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        String userEmail = authentication.getName();
        Page<OrderSummaryResponse> myOrders = orderService.getMyOrders(userEmail, pageable);
        return ResponseEntity.ok(myOrders);
    }

    /**
     * API LẤY CHI TIẾT MỘT ĐƠN HÀNG CỤ THỂ
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrderDetail(
            @PathVariable("orderId") Long orderId, // <-- THÊM ("orderId")
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(orderService.getMyOrderDetail(userEmail, orderId));
    }

    /**
     * API CHO KHÁCH HÀNG TỰ HỦY ĐƠN HÀNG
     * (Chỉ thành công nếu đơn hàng đang ở trạng thái PENDING)
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable("orderId") Long orderId, // <-- THÊM ("orderId")
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(orderService.cancelMyOrder(userEmail, orderId));
    }
}