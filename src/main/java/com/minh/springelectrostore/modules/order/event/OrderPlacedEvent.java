package com.minh.springelectrostore.modules.order.event;

import com.minh.springelectrostore.modules.order.entity.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Sự kiện được bắn ra khi một đơn hàng được đặt thành công.
 * Kế thừa ApplicationEvent của Spring.
 */
@Getter
public class OrderPlacedEvent extends ApplicationEvent {

    private final Order order;

    // Constructor nhận vào nguồn phát sự kiện (source) và đối tượng Order
    public OrderPlacedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}