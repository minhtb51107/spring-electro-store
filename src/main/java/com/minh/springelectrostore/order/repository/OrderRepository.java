package com.minh.springelectrostore.order.repository;

import com.minh.springelectrostore.order.entity.Order;
import com.minh.springelectrostore.order.entity.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>,JpaSpecificationExecutor<Order> {

    /**
     * Tìm tất cả đơn hàng của một khách hàng,
     * sắp xếp theo ngày tạo mới nhất.
     * Dùng cho trang "Lịch sử đơn hàng".
     */
    Page<Order> findByCustomer_IdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);

    /**
     * Lấy chi tiết một đơn hàng CÙNG VỚI các items của nó.
     * Dùng JOIN FETCH để tránh lỗi N+1 Query khi gọi order.getItems().
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :orderId AND o.customer.id = :customerId")
    Optional<Order> findByIdAndCustomerIdWithItems(@Param("orderId") Long orderId, @Param("customerId") Integer customerId);
    
    /**
     * Lấy chi tiết một đơn hàng bằng ID (dùng cho Admin).
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
    
    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.customer c " +
            "JOIN FETCH c.user " +
            "WHERE o.id = :orderId")
     Optional<Order> findByIdWithCustomer(@Param("orderId") Long orderId);

    /**
     * KỸ THUẬT "VERIFIED PURCHASE":
     * Kiểm tra xem User (theo email) đã từng mua thành công (DELIVERED)
     * một sản phẩm (productId) nào đó hay chưa.
     */
    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END
            FROM Order o
            JOIN o.items oi
            WHERE o.customer.user.email = :userEmail
            AND oi.productVariant.product.id = :productId
            AND o.status = 'DELIVERED'
        """)
        boolean hasUserPurchasedProduct(@Param("userEmail") String userEmail, @Param("productId") Long productId);
    
    /**
     * Tìm các đơn hàng theo trạng thái và được tạo TRƯỚC một thời điểm cụ thể.
     * Ví dụ: Tìm đơn PENDING tạo trước (hiện tại - 15 phút).
     */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, OffsetDateTime dateTime);
}