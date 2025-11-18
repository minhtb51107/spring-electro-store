package com.minh.springelectrostore.payment.controller;

import com.minh.springelectrostore.order.entity.Order;
import com.minh.springelectrostore.order.repository.OrderRepository;
import com.minh.springelectrostore.payment.dto.PaymentCallbackResponse;
import com.minh.springelectrostore.payment.dto.PaymentResponse;
import com.minh.springelectrostore.payment.service.PaymentService;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @GetMapping("/create-payment/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable("orderId") Long orderId, // <-- THÊM ("orderId")
            Authentication authentication,
            HttpServletRequest request) {
        
        // Dùng hàm fetch join để lấy luôn customer và user, tránh lỗi Lazy
        Order order = orderRepository.findByIdWithCustomer(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));
        
        String currentUserEmail = authentication.getName();
        
        // Check quyền sở hữu
        if (!order.getCustomer().getUser().getEmail().equals(currentUserEmail)) {
             throw new BadRequestException("Bạn không có quyền thanh toán cho đơn hàng này.");
        }

        // Nếu đơn hàng đã thanh toán rồi hoặc hủy thì không cho thanh toán lại
        // (Logic này nên có thêm)
        
        long amount = order.getTotalPrice().longValue();
        // Nếu có finalPrice (sau khi dùng voucher), hãy dùng finalPrice
        if (order.getFinalPrice() != null) {
            amount = order.getFinalPrice().longValue();
        }

        PaymentResponse paymentRes = paymentService.createVnPayPayment(orderId, amount, request);

        return ResponseEntity.ok(paymentRes);
    }
    
    /**
     * API Xử lý kết quả thanh toán (Callback).
     * API này KHÔNG cần đăng nhập (@PreAuthorize) vì VNPay (hoặc Frontend chưa login) sẽ gọi nó.
     * Tuy nhiên, để an toàn, ta nên kiểm tra kỹ. Trong môi trường test này, ta để public.
     */
    @GetMapping("/vn-pay-callback")
    public ResponseEntity<PaymentCallbackResponse> payCallbackHandler(HttpServletRequest request) {
        PaymentCallbackResponse response = paymentService.processVnPayCallback(request);
        
        if ("00".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }   
}