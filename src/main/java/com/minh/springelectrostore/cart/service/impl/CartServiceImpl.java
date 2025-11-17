package com.minh.springelectrostore.cart.service.impl;

import com.minh.springelectrostore.cart.dto.request.CartItemRequest;
import com.minh.springelectrostore.cart.dto.request.UpdateCartItemRequest;
import com.minh.springelectrostore.cart.dto.response.CartItemResponse;
import com.minh.springelectrostore.cart.dto.response.CartResponse;
import com.minh.springelectrostore.cart.service.CartService;
import com.minh.springelectrostore.product.entity.ProductImage;
import com.minh.springelectrostore.product.entity.ProductVariant;
import com.minh.springelectrostore.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    // (1) Inject RedisTemplate (đã cấu hình JSON)
    private final RedisTemplate<String, Object> redisTemplate;
    
    // (2) Inject Repository của Postgres (để kiểm tra)
    private final ProductVariantRepository productVariantRepository;

    // (3) Định nghĩa Key cho Redis
    private static final String CART_PREFIX = "cart:";
    private static final long CART_EXPIRATION_DAYS = 7;

    /**
     * Lấy giỏ hàng từ Redis.
     */
    @Override
    public CartResponse getCart(String userEmail) {
        String cartKey = getCartKey(userEmail);
        
        // Dùng redisTemplate.opsForValue().get() để lấy
        CartResponse cart = (CartResponse) redisTemplate.opsForValue().get(cartKey);

        // Nếu user chưa có giỏ hàng (null), trả về giỏ hàng rỗng
        if (cart == null) {
            return new CartResponse();
        }
        
        // Gia hạn thời gian sống của giỏ hàng mỗi khi user xem
        redisTemplate.expire(cartKey, CART_EXPIRATION_DAYS, TimeUnit.DAYS);
        return cart;
    }

    /**
     * Thêm/Cập nhật sản phẩm vào giỏ hàng.
     */
    @Override
    @Transactional(readOnly = true) // Chỉ đọc từ CSDL (Postgres), nhưng sẽ ghi vào Redis
    public CartResponse addItemToCart(String userEmail, CartItemRequest request) {
        // 1. Kiểm tra CSDL (Postgres): Sản phẩm có tồn tại và còn hàng không?
        ProductVariant variant = findVariant(request.getProductVariantId());
        
        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Số lượng tồn kho không đủ. Chỉ còn " + variant.getStockQuantity() + " sản phẩm.");
        }

        // 2. Lấy giỏ hàng hiện tại từ Redis
        String cartKey = getCartKey(userEmail);
        CartResponse cart = getCart(userEmail); // Dùng hàm getCart nội bộ

        // 3. Logic "Pro": Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        Optional<CartItemResponse> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductVariantId().equals(request.getProductVariantId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // --- Nếu ĐÃ CÓ: Cập nhật số lượng ---
            CartItemResponse item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            // Kiểm tra lại tồn kho với số lượng mới
            if (variant.getStockQuantity() < newQuantity) {
                throw new BadRequestException("Không thể thêm. Số lượng tồn kho không đủ (cần " + newQuantity + ", còn " + variant.getStockQuantity() + ").");
            }
            item.setQuantity(newQuantity);
            
        } else {
            // --- Nếu CHƯA CÓ: Tạo mới CartItemResponse ---
            CartItemResponse newItem = buildCartItemResponse(variant, request.getQuantity());
            cart.getItems().add(newItem);
        }

        // 4. Tính toán lại tổng tiền và lưu vào Redis
        return recalculateAndSaveCart(cartKey, cart);
    }

    /**
     * Cập nhật số lượng
     */
    @Override
    @Transactional(readOnly = true)
    public CartResponse updateItemQuantity(String userEmail, Long productVariantId, UpdateCartItemRequest request) {
        // 1. Lấy giỏ hàng
        String cartKey = getCartKey(userEmail);
        CartResponse cart = getCart(userEmail);

        // 2. Tìm món hàng
        CartItemResponse itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getProductVariantId().equals(productVariantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng."));

        // 3. Kiểm tra tồn kho (luôn kiểm tra với CSDL chính)
        ProductVariant variant = findVariant(productVariantId);
        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Số lượng tồn kho không đủ. Chỉ còn " + variant.getStockQuantity() + " sản phẩm.");
        }
        
        // 4. Cập nhật số lượng
        itemToUpdate.setQuantity(request.getQuantity());

        // 5. Tính toán lại và lưu
        return recalculateAndSaveCart(cartKey, cart);
    }

    /**
     * Xóa 1 món hàng
     */
    @Override
    public CartResponse removeItemFromCart(String userEmail, Long productVariantId) {
        String cartKey = getCartKey(userEmail);
        CartResponse cart = getCart(userEmail);

        // Xóa món hàng khỏi List
        boolean removed = cart.getItems()
                .removeIf(item -> item.getProductVariantId().equals(productVariantId));

        if (!removed) {
            throw new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng.");
        }

        // Tính toán lại và lưu
        return recalculateAndSaveCart(cartKey, cart);
    }

    /**
     * Xóa sạch giỏ hàng
     */
    @Override
    public void clearCart(String userEmail) {
        String cartKey = getCartKey(userEmail);
        redisTemplate.delete(cartKey);
        log.info("Đã xóa sạch giỏ hàng cho user: {}", userEmail);
    }


    // --- HÀM HELPER (PRIVATE) ---

    // Định nghĩa key trong Redis
    private String getCartKey(String userEmail) {
        return CART_PREFIX + userEmail;
    }

    // Hàm then chốt: Tìm sản phẩm trong CSDL
    private ProductVariant findVariant(Long productVariantId) {
        // Dùng hàm JOIN FETCH "Pro" mà chúng ta đã tạo
        return productVariantRepository.findByIdWithProductAndImages(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm với ID: " + productVariantId));
    }

    // Hàm then chốt: Tính toán lại tổng tiền
    private CartResponse recalculateAndSaveCart(String cartKey, CartResponse cart) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItemResponse item : cart.getItems()) {
            // Cập nhật tổng tiền của từng dòng (Giá * Số lượng)
            item.setLineTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            
            // Cộng dồn vào tổng tiền của giỏ hàng
            totalPrice = totalPrice.add(item.getLineTotal());
            totalItems += item.getQuantity();
        }

        cart.setTotalPrice(totalPrice);
        cart.setTotalItems(totalItems);

        // --- LƯU VÀO REDIS ---
        // Dùng redisTemplate.opsForValue().set() để lưu
        // Chúng ta đã cấu hình RedisConfig, nên 'cart' (Object) sẽ tự động
        // được chuyển thành JSON trước khi lưu.
        redisTemplate.opsForValue().set(cartKey, cart, CART_EXPIRATION_DAYS, TimeUnit.DAYS);
        log.info("Đã cập nhật giỏ hàng cho key: {}", cartKey);

        return cart;
    }

    // Hàm helper: Chuyển ProductVariant (từ CSDL) sang CartItemResponse (để lưu vào Redis)
    private CartItemResponse buildCartItemResponse(ProductVariant variant, int quantity) {
        // Lấy ảnh thumbnail (logic từ Module 2)
        String thumbnailUrl = variant.getImages().stream()
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(variant.getImages().stream() // Nếu không có, lấy ảnh đầu tiên
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null)); // Hoặc null nếu không có ảnh nào

        return CartItemResponse.builder()
                .productVariantId(variant.getId())
                .sku(variant.getSku())
                .productName(variant.getProduct().getName()) // Lấy tên từ "cha"
                .productSlug(variant.getProduct().getSlug()) // Lấy slug từ "cha"
                .price(variant.getPrice()) // Lấy giá từ chính nó
                .thumbnailUrl(thumbnailUrl)
                .quantity(quantity)
                .build();
    }
}