package com.minh.springelectrostore.modules.promotion.strategy;

import com.minh.springelectrostore.modules.promotion.entity.Voucher;
import com.minh.springelectrostore.modules.promotion.strategy.DiscountStrategy;

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