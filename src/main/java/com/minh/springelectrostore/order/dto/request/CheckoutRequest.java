package com.minh.springelectrostore.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 255, message = "Tên người nhận quá dài")
    private String customerName;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(max = 500, message = "Địa chỉ quá dài")
    private String shippingAddress;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại quá dài")
    private String shippingPhone;

    private String notes;

    // --- BỔ SUNG TRƯỜNG NÀY ---
    private String voucherCode; // Mã giảm giá (Optional)
    
    private Long addressId;
}