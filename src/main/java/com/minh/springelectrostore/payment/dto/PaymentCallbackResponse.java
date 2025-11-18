package com.minh.springelectrostore.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCallbackResponse {
    private String status; // "00" là thành công
    private String message;
    private String orderId;
    private String amount;
    private String paymentDate;
}