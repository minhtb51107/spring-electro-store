package com.minh.springelectrostore.promotion.service;

import com.minh.springelectrostore.promotion.entity.Voucher;
import com.minh.springelectrostore.promotion.entity.VoucherUsage;
import com.minh.springelectrostore.promotion.repository.VoucherRepository;
import com.minh.springelectrostore.promotion.repository.VoucherUsageRepository;
import com.minh.springelectrostore.promotion.strategy.DiscountStrategy;
import com.minh.springelectrostore.promotion.strategy.DiscountStrategyFactory;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.user.entity.User;
import com.minh.springelectrostore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.minh.springelectrostore.promotion.dto.request.VoucherRequest;
import com.minh.springelectrostore.promotion.dto.response.VoucherResponse;
import com.minh.springelectrostore.promotion.mapper.VoucherMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final UserRepository userRepository;
    private final DiscountStrategyFactory strategyFactory;
    private final VoucherMapper voucherMapper;

    /**
     * Tính toán giảm giá (Chỉ tính toán, chưa trừ lượt dùng).
     */
    public BigDecimal calculateDiscount(String code, BigDecimal orderTotal, String userEmail) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher không tồn tại: " + code));

        // 1. Validate cơ bản
        validateVoucher(voucher, orderTotal);

        // 2. Validate lịch sử dùng của User
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (voucherUsageRepository.existsByUserIdAndVoucherId(user.getId(), voucher.getId())) {
             throw new BadRequestException("Bạn đã sử dụng mã giảm giá này rồi.");
        }

        // 3. Tính toán
        DiscountStrategy strategy = strategyFactory.getStrategy(voucher.getDiscountType());
        return strategy.calculateDiscount(voucher, orderTotal);
    }

    /**
     * Áp dụng Voucher (Trừ lượt dùng và lưu lịch sử).
     * Gọi sau khi Order đã lưu thành công.
     */
    @Transactional
    public void applyVoucher(String code, String userEmail, Long orderId) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        // 1. Tăng usedCount (Atomic Update)
        int updated = voucherRepository.incrementUsage(voucher.getId());
        if (updated == 0) {
            throw new BadRequestException("Rất tiếc, voucher đã hết lượt sử dụng ngay lúc này.");
        }

        // 2. Lưu lịch sử dùng
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        VoucherUsage usage = VoucherUsage.builder()
                .voucher(voucher)
                .user(user)
                .orderId(orderId)
                .build();
        voucherUsageRepository.save(usage);
    }
    
    /**
     * Hoàn lại Voucher (Khi hủy đơn).
     */
    @Transactional
    public void refundVoucher(Long orderId) {
        voucherUsageRepository.findByOrderId(orderId).ifPresent(usage -> {
            // 1. Giảm usedCount
            voucherRepository.decrementUsage(usage.getVoucher().getId());
            
            // 2. Xóa lịch sử dùng
            voucherUsageRepository.delete(usage);
        });
    }

    private void validateVoucher(Voucher voucher, BigDecimal orderTotal) {
        if (!voucher.isActive()) {
            throw new BadRequestException("Voucher đã bị vô hiệu hóa.");
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
             throw new BadRequestException("Voucher chưa đến đợt áp dụng.");
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
             throw new BadRequestException("Voucher đã hết hạn.");
        }

        // Check sơ bộ (check kỹ hơn ở bước incrementUsage)
        if (voucher.getUsageLimit() > 0 && voucher.getUsedCount() >= voucher.getUsageLimit()) {
             throw new BadRequestException("Voucher đã hết lượt sử dụng.");
        }

        if (voucher.getMinOrderAmount() != null && orderTotal.compareTo(voucher.getMinOrderAmount()) < 0) {
             throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu để dùng voucher này.");
        }
    }
    
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchers(Pageable pageable) {
        return voucherRepository.findAll(pageable)
                .map(voucherMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Long id) {
        return voucherRepository.findById(id)
                .map(voucherMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));
    }

    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        if (voucherRepository.findByCode(request.getCode()).isPresent()) {
            throw new BadRequestException("Mã voucher '" + request.getCode() + "' đã tồn tại.");
        }
        
        Voucher voucher = voucherMapper.toEntity(request);
        voucher.setUsedCount(0); // Khởi tạo
        
        return voucherMapper.toResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherResponse updateVoucher(Long id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        // Check trùng code nếu đổi code
        if (!voucher.getCode().equals(request.getCode()) && 
            voucherRepository.findByCode(request.getCode()).isPresent()) {
            throw new BadRequestException("Mã voucher '" + request.getCode() + "' đã tồn tại.");
        }

        voucherMapper.updateEntity(request, voucher);
        return voucherMapper.toResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public void deleteVoucher(Long id) {
        if (!voucherRepository.existsById(id)) {
            throw new ResourceNotFoundException("Voucher not found");
        }
        // Soft delete: Chỉ set active = false (để giữ lịch sử đơn hàng)
        // Hoặc Hard delete nếu chưa có đơn nào dùng (phức tạp hơn)
        // Ở đây mình chọn hard delete cho đơn giản, nhưng thực tế nên soft delete.
        voucherRepository.deleteById(id);
    }
}