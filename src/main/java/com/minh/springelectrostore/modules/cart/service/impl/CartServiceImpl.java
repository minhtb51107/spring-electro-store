package com.minh.springelectrostore.modules.cart.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.common.exception.ResourceNotFoundException;
import com.minh.springelectrostore.modules.cart.dto.request.CartItemRequest;
import com.minh.springelectrostore.modules.cart.dto.request.UpdateCartItemRequest;
import com.minh.springelectrostore.modules.cart.dto.response.CartItemResponse;
import com.minh.springelectrostore.modules.cart.dto.response.CartResponse;
import com.minh.springelectrostore.modules.cart.service.CartService;
import com.minh.springelectrostore.modules.product.entity.ProductImage;
import com.minh.springelectrostore.modules.product.entity.ProductVariant;
import com.minh.springelectrostore.modules.product.repository.ProductVariantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductVariantRepository productVariantRepository;

    private static final String CART_PREFIX = "cart:";
    // [MỚI] Key Set chứa danh sách email người dùng đang có giỏ hàng active
    private static final String ACTIVE_CARTS_KEY = "cart:active_users";
    private static final long CART_EXPIRATION_DAYS = 7;

    @Override
    public CartResponse getCart(String userEmail) {
        String cartKey = getCartKey(userEmail);
        CartResponse cart = (CartResponse) redisTemplate.opsForValue().get(cartKey);

        if (cart == null) {
            return new CartResponse();
        }
        
        redisTemplate.expire(cartKey, CART_EXPIRATION_DAYS, TimeUnit.DAYS);
        return cart;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse addItemToCart(String userEmail, CartItemRequest request) {
        ProductVariant variant = findVariant(request.getProductVariantId());
        
        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Số lượng tồn kho không đủ. Chỉ còn " + variant.getStockQuantity());
        }

        String cartKey = getCartKey(userEmail);
        CartResponse cart = getCart(userEmail);

        Optional<CartItemResponse> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductVariantId().equals(request.getProductVariantId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItemResponse item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            if (variant.getStockQuantity() < newQuantity) {
                throw new BadRequestException("Tồn kho không đủ (cần " + newQuantity + ", còn " + variant.getStockQuantity() + ")");
            }
            item.setQuantity(newQuantity);
        } else {
            CartItemResponse newItem = buildCartItemResponse(variant, request.getQuantity());
            cart.getItems().add(newItem);
        }

        return recalculateAndSaveCart(cartKey, cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse updateItemQuantity(String userEmail, Long productVariantId, UpdateCartItemRequest request) {
        String cartKey = getCartKey(userEmail);
        CartResponse cart = getCart(userEmail);

        CartItemResponse itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getProductVariantId().equals(productVariantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng."));

        ProductVariant variant = findVariant(productVariantId);
        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Số lượng tồn kho không đủ.");
        }
        
        itemToUpdate.setQuantity(request.getQuantity());
        return recalculateAndSaveCart(cartKey, cart);
    }

    @Override
    public CartResponse removeItemFromCart(String userEmail, Long productVariantId) {
        String cartKey = getCartKey(userEmail);
        CartResponse cart = getCart(userEmail);

        boolean removed = cart.getItems()
                .removeIf(item -> item.getProductVariantId().equals(productVariantId));

        if (!removed) {
            throw new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng.");
        }

        return recalculateAndSaveCart(cartKey, cart);
    }

    @Override
    public void clearCart(String userEmail) {
        String cartKey = getCartKey(userEmail);
        redisTemplate.delete(cartKey);
        
        // [MỚI] Xóa user khỏi danh sách active khi họ đã đặt hàng hoặc xóa giỏ
        // Giúp Job quét không phải check user này nữa -> Tối ưu hiệu năng
        redisTemplate.opsForSet().remove(ACTIVE_CARTS_KEY, userEmail);
        
        log.info("Đã xóa sạch giỏ hàng và inactive user: {}", userEmail);
    }

    // --- HÀM HELPER ---

    private String getCartKey(String userEmail) {
        return CART_PREFIX + userEmail;
    }

    private ProductVariant findVariant(Long productVariantId) {
        return productVariantRepository.findByIdWithProductAndImages(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm: " + productVariantId));
    }

    private CartResponse recalculateAndSaveCart(String cartKey, CartResponse cart) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItemResponse item : cart.getItems()) {
            item.setLineTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalPrice = totalPrice.add(item.getLineTotal());
            totalItems += item.getQuantity();
        }

        cart.setTotalPrice(totalPrice);
        cart.setTotalItems(totalItems);

        // [MỚI] Cập nhật timestamp và reset trạng thái nhắc nhở
        cart.setLastUpdatedAt(Instant.now());
        cart.setReminderSent(false); // Reset để nếu user quay lại sửa giỏ, sau này quên tiếp thì vẫn nhắc lại

        redisTemplate.opsForValue().set(cartKey, cart, CART_EXPIRATION_DAYS, TimeUnit.DAYS);
        
        // [MỚI] Thêm user vào danh sách Active để Job quét
        // Lấy email từ cartKey (cart:email) -> email
        String email = cartKey.replace(CART_PREFIX, "");
        redisTemplate.opsForSet().add(ACTIVE_CARTS_KEY, email);

        log.info("Đã cập nhật giỏ hàng và active user: {}", email);
        return cart;
    }

    private CartItemResponse buildCartItemResponse(ProductVariant variant, int quantity) {
        String thumbnailUrl = variant.getImages().stream()
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(variant.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null));

        return CartItemResponse.builder()
                .productVariantId(variant.getId())
                .sku(variant.getSku())
                .productName(variant.getProduct().getName())
                .productSlug(variant.getProduct().getSlug())
                .price(variant.getPrice())
                .thumbnailUrl(thumbnailUrl)
                .quantity(quantity)
                .build();
    }
}