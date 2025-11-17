package com.minh.springelectrostore.product.repository;

import com.minh.springelectrostore.product.dto.request.ProductSearchCriteria;
import com.minh.springelectrostore.product.entity.Product;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery; // <-- IMPORT CHÍNH XÁC
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp này xây dựng câu lệnh WHERE động cho Product dựa trên ProductSearchCriteria.
 * Đây là kỹ thuật "JPA Specifications"
 */
public class ProductSpecification implements Specification<Product> {

    private final ProductSearchCriteria criteria;
    private final List<Predicate> predicates = new ArrayList<>();

    public ProductSpecification(ProductSearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        
        // 1. Chỉ lấy các sản phẩm đang active (luôn luôn)
        predicates.add(cb.isTrue(root.get("active")));
        
        // 2. Lọc theo 'name' (dùng LIKE)
        if (StringUtils.hasText(criteria.getName())) {
            predicates.add(
                cb.like(cb.lower(root.get("name")), "%" + criteria.getName().toLowerCase() + "%")
            );
        }

        // 3. Lọc theo 'categorySlug' (dùng JOIN)
        if (StringUtils.hasText(criteria.getCategorySlug())) {
            predicates.add(
                cb.equal(root.join("category", JoinType.LEFT).get("slug"), criteria.getCategorySlug())
            );
        }

        // 4. Lọc theo 'brandSlug' (dùng JOIN)
        if (StringUtils.hasText(criteria.getBrandSlug())) {
            predicates.add(
                cb.equal(root.join("brand", JoinType.LEFT).get("slug"), criteria.getBrandSlug())
            );
        }
        
        // --- Phần này quan trọng: Lọc theo giá (Price) ---
        if (criteria.getPriceGte() != null || criteria.getPriceLte() != null) {
            
            // === SỬA LỖI Ở ĐÂY ===
            // Kiểu dữ liệu phải là Subquery<Long>, không phải CriteriaQuery<Long>
            Subquery<Long> subQuery = query.subquery(Long.class);
            // ======================

            // subQuery.correlate(root) dùng để liên kết truy vấn con này
            // với truy vấn cha (Root<Product> root)
            Root<Product> subRoot = subQuery.correlate(root);
            
            // JOIN vào bảng variants từ subRoot
            var subVariant = subRoot.join("variants");
            
            List<Predicate> subPredicates = new ArrayList<>();
            
            // 5. Lọc giá "Từ" (price_gte)
            if (criteria.getPriceGte() != null) {
                subPredicates.add(
                    cb.greaterThanOrEqualTo(subVariant.get("price"), criteria.getPriceGte())
                );
            }
            
            // 6. Lọc giá "Đến" (price_lte)
            if (criteria.getPriceLte() != null) {
                subPredicates.add(
                    cb.lessThanOrEqualTo(subVariant.get("price"), criteria.getPriceLte())
                );
            }
            
            // Hoàn thành Subquery
            subQuery.select(cb.literal(1L))
                    .where(subPredicates.toArray(new Predicate[0]));
                    
            // Thêm Subquery vào câu lệnh WHERE chính
            // cb.exists(subQuery) giờ sẽ hợp lệ vì subQuery là kiểu Subquery
            predicates.add(cb.exists(subQuery));
        }

        // Kết hợp tất cả các Predicate (điều kiện) lại bằng AND
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}