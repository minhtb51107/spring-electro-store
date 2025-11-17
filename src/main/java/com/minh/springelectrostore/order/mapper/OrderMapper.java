package com.minh.springelectrostore.order.mapper;

import com.minh.springelectrostore.order.dto.response.OrderItemResponse;
import com.minh.springelectrostore.order.dto.response.OrderResponse;
import com.minh.springelectrostore.order.dto.response.OrderSummaryResponse;
import com.minh.springelectrostore.order.entity.Order;
import com.minh.springelectrostore.order.entity.OrderItem;
import com.minh.springelectrostore.product.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "totalItems", expression = "java(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())")
    OrderResponse toOrderResponse(Order order);

    @Mapping(source = "productVariant.id", target = "productVariantId")
    @Mapping(source = "productVariant.sku", target = "sku")
    @Mapping(source = "productVariant.product.name", target = "productName")
    
    // --- SỬA Ở ĐÂY: Đổi "entity" thành "." ---
    @Mapping(source = ".", target = "thumbnailUrl", qualifiedByName = "mapThumbnailUrlFromItem") 
    // -----------------------------------------
    
    @Mapping(target = "lineTotal", expression = "java(entity.getPriceAtPurchase().multiply(new java.math.BigDecimal(entity.getQuantity())))")
    OrderItemResponse toOrderItemResponse(OrderItem entity);

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "totalItems", expression = "java(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())")
    OrderSummaryResponse toOrderSummaryResponse(Order order);
    
    @Named("mapThumbnailUrlFromItem") 
    default String mapThumbnailUrl(OrderItem item) {
        if (item.getProductVariant() == null || item.getProductVariant().getImages() == null) {
            return null;
        }

        return item.getProductVariant().getImages().stream()
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(item.getProductVariant().getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null));
    }
}