package com.minh.springelectrostore.promotion.strategy;

import com.minh.springelectrostore.promotion.entity.Voucher;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("PERCENTAGE") // Bean name trùng với Enum DiscountType
public class PercentageStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        // Tính % giảm: orderTotal * (value / 100)
        BigDecimal discount = orderTotal.multiply(voucher.getDiscountValue())
                .divide(BigDecimal.valueOf(100));

        // Kiểm tra Max Discount (nếu có)
        if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.min(voucher.getMaxDiscountAmount());
        }

        return discount;
    }
}