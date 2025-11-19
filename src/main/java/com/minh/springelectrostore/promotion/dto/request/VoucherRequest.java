package com.minh.springelectrostore.promotion.dto.request;

import com.minh.springelectrostore.promotion.entity.DiscountType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class VoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Loại giảm giá là bắt buộc")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm là bắt buộc")
    @Min(value = 0, message = "Giá trị giảm không được âm")
    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount; // Tùy chọn (cho %)
    private BigDecimal minOrderAmount; // Tùy chọn

    private OffsetDateTime startDate;
    
    @Future(message = "Ngày kết thúc phải ở tương lai")
    private OffsetDateTime endDate;

    @Min(0)
    private Integer usageLimit; // 0 = không giới hạn
    
    private boolean active;
}