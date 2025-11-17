package com.minh.springelectrostore.product.repository;

import com.minh.springelectrostore.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
	
	boolean existsBySku(String sku);

    /**
     * Tùy chọn: Tìm biến thể bằng SKU.
     * Rất hữu ích cho việc quản lý kho sau này.
     */
    Optional<ProductVariant> findBySku(String sku);
    
    /**
     * Lấy thông tin chi tiết của biến thể để thêm vào giỏ hàng.
     * Chúng ta dùng JOIN FETCH để lấy "product" (cha) và "images" (con)
     * trong CÙNG MỘT câu query, tránh lỗi N+1.
     */
    @Query("SELECT pv FROM ProductVariant pv " +
            "LEFT JOIN FETCH pv.product p " +
            "LEFT JOIN FETCH pv.images " +
            "WHERE pv.id = :id")
     Optional<ProductVariant> findByIdWithProductAndImages(@Param("id") Long id);
    
    /**
     * Trừ kho một cách an toàn (Atomic Update).
     * Chỉ trừ kho NẾU (WHERE) số lượng tồn kho hiện tại (stock_quantity) >= số lượng cần trừ.
     * @param variantId ID của biến thể cần trừ kho
     * @param quantity Số lượng cần trừ
     * @return số dòng đã được cập nhật (1 = thành công, 0 = thất bại/hết hàng)
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant pv " +
           "SET pv.stockQuantity = pv.stockQuantity - :quantity " +
           "WHERE pv.id = :variantId AND pv.stockQuantity >= :quantity")
    int decreaseStock(@Param("variantId") Long variantId, @Param("quantity") int quantity);
    
    /**
     * Hoàn trả tồn kho (khi hủy đơn).
     * Atomic Update: stock = stock + quantity
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant pv SET pv.stockQuantity = pv.stockQuantity + :quantity WHERE pv.id = :id")
    void increaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}