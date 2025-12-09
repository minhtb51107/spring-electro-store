package com.minh.springelectrostore.modules.payment.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.common.exception.ResourceNotFoundException;
import com.minh.springelectrostore.config.VnPayConfig;
import com.minh.springelectrostore.modules.order.entity.Order;
import com.minh.springelectrostore.modules.order.entity.OrderItem;
import com.minh.springelectrostore.modules.order.entity.OrderStatus;
import com.minh.springelectrostore.modules.order.repository.OrderRepository;
import com.minh.springelectrostore.modules.payment.dto.response.PaymentCallbackResponse;
import com.minh.springelectrostore.modules.payment.dto.response.PaymentResponse;
import com.minh.springelectrostore.modules.payment.service.PaymentService;
import com.minh.springelectrostore.modules.product.service.InventoryService;
import com.minh.springelectrostore.modules.promotion.service.VoucherService;
import com.minh.springelectrostore.modules.notification.service.NotificationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String secretKey;

    @Value("${vnpay.url}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Value("${vnpay.command}")
    private String vnpCommand;
    
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    
    // [QUAN TRỌNG] Inject Service để xử lý hoàn kho/voucher
    private final InventoryService inventoryService;
    private final VoucherService voucherService;

    @Override
    public PaymentResponse createVnPayPayment(Long orderId, long amount, HttpServletRequest request) {
        String orderType = "other";
        long amountInVnpFormat = amount * 100; // VNPay yêu cầu nhân 100
        
        String vnp_TxnRef = String.valueOf(orderId);
        String vnp_IpAddr = VnPayConfig.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpVersion);
        vnp_Params.put("vnp_Command", vnpCommand);
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVnpFormat));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang #" + orderId);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                // Build query string
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayConfig.hmacSHA512(secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnpPayUrl + "?" + queryUrl;

        return PaymentResponse.builder()
                .orderId(vnp_TxnRef)
                .paymentUrl(paymentUrl)
                .build();
    }
    
    /**
     * Xử lý Callback từ VNPay (IPN).
     * Hàm này quan trọng nhất: Cần xử lý Transaction, Idempotency và Security.
     */
    @Override
    @Transactional // Đảm bảo cập nhật DB an toàn (Order, Inventory, Voucher)
    public PaymentCallbackResponse processVnPayCallback(HttpServletRequest request) {
        // 1. Lấy tham số và Verify Chữ ký (Checksum)
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) fields.remove("vnp_SecureHashType");
        if (fields.containsKey("vnp_SecureHash")) fields.remove("vnp_SecureHash");

        String signValue = hashAllFields(fields);
        if (!signValue.equals(vnp_SecureHash)) {
            log.error("Checksum VNPay không hợp lệ!");
            return PaymentCallbackResponse.builder().status("99").message("Chữ ký không hợp lệ").build();
        }

        // 2. Lấy thông tin đơn hàng
        String orderIdStr = request.getParameter("vnp_TxnRef");
        String amountStr = request.getParameter("vnp_Amount");
        String responseCode = request.getParameter("vnp_ResponseCode");
        
        Long orderId = Long.parseLong(orderIdStr);
        Order order = orderRepository.findByIdWithItems(orderId) // Fetch items để xử lý kho nếu cần
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        // 3. [IDEMPOTENCY] Kiểm tra xem đơn hàng đã được xử lý trước đó chưa?
        // Nếu đã PAID hoặc CANCELLED thì không làm gì cả, trả về thành công để VNPay không gọi lại.
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Đơn hàng #{} đã được xử lý trước đó (Status: {}). Bỏ qua callback.", orderId, order.getStatus());
            return PaymentCallbackResponse.builder().status("00").message("Đơn hàng đã được xử lý").orderId(orderIdStr).build();
        }

        // 4. [SECURITY] Validate số tiền (Chống hack sửa giá request)
        long vnpAmount = Long.parseLong(amountStr) / 100;
        if (vnpAmount != order.getFinalPrice().longValue()) {
            log.error("Cảnh báo: Sai lệch số tiền! Order: {}, VNPay: {}", order.getFinalPrice(), vnpAmount);
            throw new BadRequestException("Số tiền thanh toán không khớp!");
        }

        // 5. Xử lý kết quả
        if ("00".equals(responseCode)) {
            // --- THANH TOÁN THÀNH CÔNG ---
            log.info("Thanh toán THÀNH CÔNG cho đơn hàng ID: {}", orderId);
            
            order.setStatus(OrderStatus.PAID); // Hoặc PROCESSING/SHIPPING
            // order.setPaymentMethod("VNPAY"); // Nếu bạn có trường này
            orderRepository.save(order);
            
            // Gửi email xác nhận
            try {
                String userEmail = order.getCustomer().getUser().getEmail();
                notificationService.sendNotificationToUser(userEmail, "Thanh toán thành công đơn hàng #" + orderId);
            } catch (Exception e) {
                log.warn("Lỗi gửi email: {}", e.getMessage());
            }

        } else {
            // --- THANH TOÁN THẤT BẠI (Hủy, Lỗi thẻ, Hết tiền...) ---
            log.warn("Thanh toán THẤT BẠI cho đơn hàng ID: {}. Mã lỗi: {}", orderId, responseCode);
            
            order.setStatus(OrderStatus.CANCELLED);
            order.setNotes(order.getNotes() + " [VNPay: Thanh toán thất bại - Mã lỗi: " + responseCode + "]");
            orderRepository.save(order);

            // [QUAN TRỌNG] Hoàn trả hàng vào kho
            for (OrderItem item : order.getItems()) {
                inventoryService.restoreStock(item.getProductVariant().getId(), item.getQuantity());
            }

            // [QUAN TRỌNG] Hoàn lại Voucher
            voucherService.refundVoucher(orderId);
        }

        return PaymentCallbackResponse.builder()
                .status(responseCode)
                .message("00".equals(responseCode) ? "Thanh toán thành công" : "Thanh toán thất bại")
                .orderId(orderIdStr)
                .amount(amountStr)
                .build();
    }

    private String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append('=');
                try {
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    sb.append('&');
                }
            }
        }
        return VnPayConfig.hmacSHA512(secretKey, sb.toString());
    }
}