package com.minh.springelectrostore.order.repository;

import com.minh.springelectrostore.order.dto.request.AdminOrderSearchCriteria;
import com.minh.springelectrostore.order.entity.Order;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AdminOrderSpecification implements Specification<Order> {

    private final AdminOrderSearchCriteria criteria;
    private final List<Predicate> predicates = new ArrayList<>();

    public AdminOrderSpecification(AdminOrderSearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        
        // 1. Lọc theo ID đơn hàng
        if (criteria.getOrderId() != null) {
            predicates.add(cb.equal(root.get("id"), criteria.getOrderId()));
        }

        // 2. Lọc theo Trạng thái (Status)
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }

        // 3. Lọc theo Email khách hàng (phải JOIN vào Customer -> User)
        if (StringUtils.hasText(criteria.getCustomerEmail())) {
            Join<Object, Object> customerJoin = root.join("customer", JoinType.LEFT);
            Join<Object, Object> userJoin = customerJoin.join("user", JoinType.LEFT);
            
            predicates.add(
                cb.like(cb.lower(userJoin.get("email")), "%" + criteria.getCustomerEmail().toLowerCase() + "%")
            );
        }

        // 4. Lọc theo Ngày "Từ" (FromDate)
        if (criteria.getFromDate() != null) {
            predicates.add(
                cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getFromDate())
            );
        }

        // 5. Lọc theo Ngày "Đến" (ToDate)
        if (criteria.getToDate() != null) {
            predicates.add(
                cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getToDate())
            );
        }

        // Đảm bảo kết quả là duy nhất (vì có JOIN)
        query.distinct(true);
        
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}