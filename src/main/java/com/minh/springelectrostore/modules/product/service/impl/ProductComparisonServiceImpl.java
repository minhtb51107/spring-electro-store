package com.minh.springelectrostore.modules.product.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.modules.product.dto.response.ProductComparisonResponse;
import com.minh.springelectrostore.modules.product.entity.Product;
import com.minh.springelectrostore.modules.product.entity.ProductAttribute;
import com.minh.springelectrostore.modules.product.entity.ProductImage;
import com.minh.springelectrostore.modules.product.entity.ProductVariant;
import com.minh.springelectrostore.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductComparisonServiceImpl {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductComparisonResponse compareProducts(List<Long> productIds) {
        if (productIds == null || productIds.size() < 2) {
            throw new BadRequestException("Cần ít nhất 2 sản phẩm để so sánh.");
        }
        if (productIds.size() > 4) {
            throw new BadRequestException("Chỉ có thể so sánh tối đa 4 sản phẩm.");
        }

        List<Product> products = productRepository.findAllById(productIds);
        
        // Sắp xếp lại theo thứ tự ID client gửi lên
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<Product> sortedProducts = new ArrayList<>();
        for (Long id : productIds) {
            if (productMap.containsKey(id)) {
                sortedProducts.add(productMap.get(id));
            }
        }

        // 1. Build Header (Product Summary)
        List<ProductComparisonResponse.ProductSummary> summaries = sortedProducts.stream()
            .map(p -> {
                // [FIX] Lấy giá từ biến thể đầu tiên (vì Product không có giá)
                BigDecimal displayPrice = p.getVariants().stream()
                        .findFirst()
                        .map(ProductVariant::getPrice)
                        .orElse(BigDecimal.ZERO);

                return ProductComparisonResponse.ProductSummary.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .price(displayPrice) // [FIX] Sử dụng giá vừa lấy
                    .slug(p.getSlug())
                    .thumbnailUrl(getThumbnail(p))
                    .build();
            })
            .collect(Collectors.toList());

        // 2. Build Attributes Table
        Set<String> allAttributeNames = new LinkedHashSet<>();
        sortedProducts.forEach(p -> p.getAttributes().forEach(attr -> allAttributeNames.add(attr.getName())));

        Map<String, List<String>> comparisonTable = new LinkedHashMap<>();
        
        for (String attrName : allAttributeNames) {
            List<String> values = new ArrayList<>();
            for (Product p : sortedProducts) {
                String val = p.getAttributes().stream()
                        .filter(a -> a.getName().equalsIgnoreCase(attrName))
                        .map(ProductAttribute::getValue)
                        .findFirst()
                        .orElse("-");
                values.add(val);
            }
            comparisonTable.put(attrName, values);
        }

        return ProductComparisonResponse.builder()
                .products(summaries)
                .attributes(comparisonTable)
                .build();
    }

    private String getThumbnail(Product product) {
        // [FIX] Duyệt qua các variants để tìm ảnh thumbnail
        if (product.getVariants() == null) return null;
        
        return product.getVariants().stream()
                .flatMap(v -> v.getImages().stream()) // Stream tất cả ảnh của tất cả variant
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getVariants().stream()
                        .flatMap(v -> v.getImages().stream())
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null));
    }
}