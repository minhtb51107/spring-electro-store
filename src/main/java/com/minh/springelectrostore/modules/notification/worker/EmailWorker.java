package com.minh.springelectrostore.modules.notification.worker;

import com.minh.springelectrostore.common.service.EmailService;
import com.minh.springelectrostore.modules.cart.dto.response.CartResponse; // [MỚI]
import com.minh.springelectrostore.modules.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {

    private final EmailService emailService;

    private String formatCurrency(java.math.BigDecimal amount) {
        return String.format("%,.0f đ", amount);
    }

    @Async("taskExecutor") 
    public void sendOrderConfirmationEmail(String to, Order order) {
        log.info("ASYNC EMAIL: Gửi xác nhận đơn hàng #{}", order.getId());
        try {
            String subject = "Xác nhận đơn hàng #" + order.getId() + " - Spring Electro Store";
            
            StringBuilder body = new StringBuilder();
            body.append("<html><body>");
            body.append("<h2>Cảm ơn bạn đã đặt hàng!</h2>");
            body.append("<p>Xin chào <b>").append(order.getCustomerName()).append("</b>,</p>");
            body.append("<p>Đơn hàng <b>#").append(order.getId()).append("</b> của bạn đã được đặt thành công.</p>");
            
            body.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>");
            body.append("<tr><th>Sản phẩm</th><th>SL</th><th>Giá</th></tr>");
            
            order.getItems().forEach(item -> {
                body.append("<tr>");
                body.append("<td>").append(item.getProductVariant().getProduct().getName())
                    .append(" (").append(item.getProductVariant().getSku()).append(")</td>");
                body.append("<td>").append(item.getQuantity()).append("</td>");
                body.append("<td>").append(formatCurrency(item.getPriceAtPurchase())).append("</td>");
                body.append("</tr>");
            });
            body.append("</table>");
            
            body.append("<p><b>Tổng thanh toán: ").append(formatCurrency(order.getFinalPrice())).append("</b></p>");
            body.append("<p>Địa chỉ giao hàng: ").append(order.getShippingAddress()).append("</p>");
            body.append("<p>Cảm ơn quý khách!</p>");
            body.append("</body></html>");
            
            emailService.sendEmail(to, subject, body.toString());
            log.info("ASYNC EMAIL: Gửi thành công tới {}", to);
        } catch (Exception e) {
            log.error("ASYNC EMAIL FAILED: Đơn hàng #{}", order.getId(), e);
        }
    }

    // [MỚI] Hàm gửi mail nhắc giỏ hàng
    @Async("taskExecutor")
    public void sendAbandonedCartEmail(String to, CartResponse cart) {
        log.info("ASYNC EMAIL: Gửi nhắc nhở giỏ hàng tới {}", to);
        try {
            String subject = "Bạn để quên đồ trong giỏ hàng kìa! - Spring Electro Store";
            
            StringBuilder body = new StringBuilder();
            body.append("<html><body>");
            body.append("<h2>Đừng bỏ lỡ món đồ yêu thích của bạn!</h2>");
            body.append("<p>Chúng tôi nhận thấy bạn chưa hoàn tất đơn hàng. Các sản phẩm này đang chờ bạn:</p>");
            
            body.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>");
            body.append("<tr><th>Sản phẩm</th><th>SL</th><th>Giá</th></tr>");
            
            cart.getItems().forEach(item -> {
                body.append("<tr>");
                body.append("<td>").append(item.getProductName()).append("</td>");
                body.append("<td>").append(item.getQuantity()).append("</td>");
                body.append("<td>").append(formatCurrency(item.getPrice())).append("</td>");
                body.append("</tr>");
            });
            body.append("</table>");
            
            body.append("<p><b>Tổng trị giá: ").append(formatCurrency(cart.getTotalPrice())).append("</b></p>");
            // Link này bạn có thể thay bằng domain thật hoặc lấy từ config
            body.append("<p><a href='http://localhost:3000/cart' style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Bấm vào đây để thanh toán ngay</a></p>");
            body.append("<p><i>(Nếu bạn đã mua hàng rồi, hãy bỏ qua email này nhé)</i></p>");
            body.append("</body></html>");
            
            emailService.sendEmail(to, subject, body.toString());
            log.info("ASYNC EMAIL: Đã gửi nhắc nhở thành công tới {}", to);
            
        } catch (Exception e) {
            log.error("ASYNC EMAIL FAILED: Lỗi gửi mail nhắc giỏ hàng tới {}", to, e);
        }
    }
}