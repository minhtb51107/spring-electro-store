package com.minh.springelectrostore.promotion.strategy;

import com.minh.springelectrostore.promotion.entity.DiscountType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DiscountStrategyFactory {

    // Spring sẽ tự động inject tất cả các bean implement DiscountStrategy vào Map này
    // Key là tên Bean ("PERCENTAGE", "FIXED_AMOUNT"), Value là instance tương ứng.
    private final Map<String, DiscountStrategy> strategyMap;

    public DiscountStrategy getStrategy(DiscountType type) {
        DiscountStrategy strategy = strategyMap.get(type.name());
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for discount type: " + type);
        }
        return strategy;
    }
}