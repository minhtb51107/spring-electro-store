package com.minh.springelectrostore.payment.service;

import com.minh.springelectrostore.payment.dto.PaymentCallbackResponse;
import com.minh.springelectrostore.payment.dto.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {

    /**
     * Tạo URL thanh toán VNPay cho một đơn hàng.
     * @param orderId ID của đơn hàng đã tạo (status = PENDING)
     * @param amount Số tiền cần thanh toán (VND)
     * @param request HttpServletRequest để lấy IP người dùng
     * @return URL thanh toán
     */
    PaymentResponse createVnPayPayment(Long orderId, long amount, HttpServletRequest request);

    /**
     * Xử lý kết quả trả về từ VNPay (Webhook/Return URL).
     * @param request Request chứa các tham số do VNPay gửi về
     * @return Kết quả xử lý (1 = Thành công, 0 = Thất bại)
     */
//    int processVnPayReturn(HttpServletRequest request);
    
//    PaymentCallbackResponse handleVnPayCallback(HttpServletRequest request);

	/**
	 * Xử lý kết quả trả về (Callback)
	 */
	PaymentCallbackResponse processVnPayCallback(HttpServletRequest request);
}