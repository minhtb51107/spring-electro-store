package com.minh.springelectrostore.order.service;

import com.minh.springelectrostore.order.dto.request.AdminOrderSearchCriteria;
import com.minh.springelectrostore.order.dto.request.UpdateOrderStatusRequest;
import com.minh.springelectrostore.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderService {

    /**
     * Lọc và tìm kiếm TẤT CẢ đơn hàng trong hệ thống.
     * Dùng JPA Specifications.
     * Cần quyền: VIEW_ALL_ORDERS
     *
     * @param criteria DTO chứa các tiêu chí lọc (status, email, date...)
     * @param pageable Thông tin phân trang
     * @return Một trang (Page) chứa các đơn hàng chi tiết.
     */
    Page<OrderResponse> searchOrders(AdminOrderSearchCriteria criteria, Pageable pageable);

    /**
     * Lấy chi tiết một đơn hàng bất kỳ bằng ID.
     * Cần quyền: VIEW_ALL_ORDERS
     *
     * @param orderId ID của đơn hàng
     * @return DTO chi tiết của đơn hàng.
     */
    OrderResponse getOrderDetailById(Long orderId);

    /**
     * Cập nhật trạng thái của một đơn hàng.
     * (Ví dụ: Chuyển từ PENDING -> PROCESSING -> SHIPPED)
     * Cần quyền: UPDATE_ORDER_STATUS
     *
     * @param orderId ID của đơn hàng
     * @param request DTO chứa trạng thái mới
     * @return DTO chi tiết của đơn hàng sau khi cập nhật.
     */
    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    // TODO: Thêm các hàm sau này (nếu cần)
    // OrderResponse addOrderNote(Long orderId, String note);
    // void deleteOrder(Long orderId); // (Xóa cứng, chỉ Super-Admin mới có)
}