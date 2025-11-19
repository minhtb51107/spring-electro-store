package com.minh.springelectrostore.promotion.strategy;

import com.minh.springelectrostore.promotion.entity.Voucher;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("FIXED_AMOUNT") // Bean name trùng với Enum DiscountType
public class FixedAmountStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        // Giảm đúng số tiền đã định, nhưng không vượt quá tổng tiền đơn hàng
        return voucher.getDiscountValue().min(orderTotal);
    }
}