package com.minh.springelectrostore.modules.search.service.impl;

import com.minh.springelectrostore.common.exception.ResourceNotFoundException;
import com.minh.springelectrostore.modules.product.entity.Product;
import com.minh.springelectrostore.modules.product.entity.ProductImage;
import com.minh.springelectrostore.modules.product.entity.ProductVariant;
import com.minh.springelectrostore.modules.product.repository.ProductRepository;
import com.minh.springelectrostore.modules.search.document.ProductDocument;
import com.minh.springelectrostore.modules.search.event.ProductSyncEvent;
import com.minh.springelectrostore.modules.search.repository.ProductSearchRepository;
import com.minh.springelectrostore.modules.search.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncServiceImpl implements ProductSyncService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;

    @Override
    public void handleProductSyncEvent(ProductSyncEvent event) {
        log.info("Xử lý sự kiện đồng bộ search cho Product ID: {}", event.getProductId());
        indexProduct(event.getProductId()); 
    }

    @Override
    @Transactional(readOnly = true)
    public void indexProduct(Long productId) {
        log.info("Đang đồng bộ sản phẩm ID: {} sang Elasticsearch...", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.isActive()) {
            deleteProductFromIndex(productId);
            return;
        }

        ProductDocument document = buildProductDocument(product);
        productSearchRepository.save(document);
        
        log.info("-> Đồng bộ thành công ID: {}", productId);
    }

    @Override
    public void deleteProductFromIndex(Long productId) {
        productSearchRepository.deleteById(productId);
        log.info("-> Đã xóa sản phẩm ID: {} khỏi Index", productId);
    }

    private ProductDocument buildProductDocument(Product product) {
        int totalStock = 0;
        BigDecimal minPrice = BigDecimal.ZERO;

        // [FIX] Lấy ảnh từ Variants thay vì từ Product trực tiếp
        String thumbnail = "";

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            totalStock = product.getVariants().stream()
                    .mapToInt(ProductVariant::getStockQuantity)
                    .sum();

            minPrice = product.getVariants().stream()
                    .map(ProductVariant::getPrice)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            
            // Logic tìm ảnh thumbnail: Duyệt qua tất cả variant, lấy ảnh thumbnail đầu tiên tìm thấy
            thumbnail = product.getVariants().stream()
                    .flatMap(v -> v.getImages().stream())
                    .filter(ProductImage::isThumbnail)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(
                        // Fallback: Lấy ảnh bất kỳ nếu không có thumbnail
                        product.getVariants().stream()
                            .flatMap(v -> v.getImages().stream())
                            .map(ProductImage::getImageUrl)
                            .findFirst()
                            .orElse("")
                    );
        }

        return ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(minPrice)
                .salePrice(minPrice) 
                .thumbnail(thumbnail) // [OK] Đã có giá trị từ logic variant ở trên
                .stockQuantity(totalStock)
                .soldQuantity(product.getSoldQuantity() != null ? product.getSoldQuantity() : 0L)
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviews() != null ? product.getReviews().size() : 0)
                .active(product.isActive())
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().toInstant() : null)
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().toInstant() : null)
                
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categorySlug(product.getCategory() != null ? product.getCategory().getSlug() : null)
                
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .brandSlug(product.getBrand() != null ? product.getBrand().getSlug() : null) // Thêm brandSlug cho đủ bộ
                
                .build();
    }
}