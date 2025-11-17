package com.minh.springelectrostore.order.controller;

import com.minh.springelectrostore.order.dto.request.AdminOrderSearchCriteria;
import com.minh.springelectrostore.order.dto.request.UpdateOrderStatusRequest;
import com.minh.springelectrostore.order.dto.response.OrderResponse;
import com.minh.springelectrostore.order.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders") 
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VIEW_ALL_ORDERS')")
    public ResponseEntity<Page<OrderResponse>> searchOrders(
            @ModelAttribute AdminOrderSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        
        Page<OrderResponse> orders = adminOrderService.searchOrders(criteria, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VIEW_ALL_ORDERS')")
    public ResponseEntity<OrderResponse> getOrderDetailById(
            @PathVariable("orderId") Long orderId) { // <--- SỬA: THÊM ("orderId")
        OrderResponse orderDetail = adminOrderService.getOrderDetailById(orderId);
        return ResponseEntity.ok(orderDetail);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('UPDATE_ORDER_STATUS')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable("orderId") Long orderId, // <--- SỬA: THÊM ("orderId")
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        
        OrderResponse updatedOrder = adminOrderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(updatedOrder);
    }
}