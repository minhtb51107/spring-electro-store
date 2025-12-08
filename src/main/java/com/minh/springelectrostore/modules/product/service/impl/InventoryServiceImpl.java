package com.minh.springelectrostore.modules.product.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.modules.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.modules.product.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final RedissonClient redissonClient;

    @Override
    // Sử dụng Propagation.MANDATORY để đảm bảo hàm này CHỈ chạy trong một Transaction có sẵn (từ OrderService)
    // Nếu gọi hàm này mà chưa mở Transaction, nó sẽ bắn lỗi. Điều này đảm bảo tính nhất quán dữ liệu.
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveStock(Long variantId, Integer quantity) {
        String lockKey = "lock:product_variant:" + variantId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Logic Lock: Chờ tối đa 2s, giữ lock tối đa 5s
            boolean isLocked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            
            if (!isLocked) {
                log.warn("Không thể acquire lock cho Variant ID: {}", variantId);
                throw new BadRequestException("Hệ thống đang bận xử lý sản phẩm này, vui lòng thử lại!");
            }

            // Logic Trừ Kho (Critical Section)
            int updatedRows = productVariantRepository.decreaseStock(variantId, quantity);
            if (updatedRows == 0) {
                log.warn("Hết hàng cho Variant ID: {}", variantId);
                // Bạn có thể query thêm tên sản phẩm để thông báo lỗi chi tiết hơn nếu muốn
                throw new BadRequestException("Sản phẩm đã hết hàng hoặc không đủ số lượng.");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Lỗi hệ thống khi xử lý kho hàng.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}