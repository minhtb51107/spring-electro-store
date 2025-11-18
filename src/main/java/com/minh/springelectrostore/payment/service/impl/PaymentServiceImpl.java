package com.minh.springelectrostore.payment.service.impl;

import com.minh.springelectrostore.config.VnPayConfig;
import com.minh.springelectrostore.notification.service.NotificationService;
import com.minh.springelectrostore.payment.dto.PaymentCallbackResponse;
import com.minh.springelectrostore.payment.dto.PaymentResponse;
import com.minh.springelectrostore.payment.service.PaymentService;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;

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

import com.minh.springelectrostore.order.entity.Order;
import com.minh.springelectrostore.order.entity.OrderStatus;
import com.minh.springelectrostore.order.repository.OrderRepository;
import com.minh.springelectrostore.payment.dto.PaymentResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    // Inject các thông số từ application.properties
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

    @Override
    public PaymentResponse createVnPayPayment(Long orderId, long amount, HttpServletRequest request) {
        // Các tham số chính của VNPay
        String orderType = "other"; // Loại hàng hóa
        
        // Quy tắc của VNPay: Số tiền phải nhân 100 (ví dụ 10.000 VND -> 1000000)
        long amountInVnpFormat = amount * 100; 
        
        String vnp_TxnRef = String.valueOf(orderId); // Mã đơn hàng của mình
        String vnp_IpAddr = VnPayConfig.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpVersion);
        vnp_Params.put("vnp_Command", vnpCommand);
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVnpFormat));
        vnp_Params.put("vnp_CurrCode", "VND");
        
        // Mã ngân hàng (để trống thì user chọn trên cổng VNPay)
        // vnp_Params.put("vnp_BankCode", "NCB"); 
        
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang #" + orderId);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Thời gian tạo
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Thời gian hết hạn (15 phút)
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // --- BƯỚC QUAN TRỌNG: TẠO CHỮ KÝ (SIGNATURE) ---
        // 1. Sắp xếp các tham số theo thứ tự a-z (bắt buộc)
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

        // 2. Tạo mã bảo mật (Secure Hash)
        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayConfig.hmacSHA512(secretKey, hashData.toString());
        
        // 3. Cộng chuỗi lại thành URL hoàn chỉnh
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnpPayUrl + "?" + queryUrl;

        return PaymentResponse.builder()
                .orderId(vnp_TxnRef)
                .paymentUrl(paymentUrl)
                .build();
    }

//    @Override
//    public int processVnPayReturn(HttpServletRequest request) {
//        // TODO: Chúng ta sẽ làm phần xử lý kết quả trả về ở bước sau
//        return 0;
//    }
//    
//    @Override
//    @Transactional
//    public PaymentCallbackResponse handleVnPayCallback(HttpServletRequest request) {
//        Map<String, String> fields = new HashMap<>();
//        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
//            String fieldName = params.nextElement();
//            String fieldValue = request.getParameter(fieldName);
//            if ((fieldValue != null) && (fieldValue.length() > 0)) {
//                fields.put(fieldName, fieldValue);
//            }
//        }
//
//        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
//        if (fields.containsKey("vnp_SecureHashType")) {
//            fields.remove("vnp_SecureHashType");
//        }
//        if (fields.containsKey("vnp_SecureHash")) {
//            fields.remove("vnp_SecureHash");
//        }
//
//        // Kiểm tra chữ ký (Checksum)
//        String rawData = VnPayConfig.hashAllFields(fields); // Hàm này trả về chuỗi chưa hash
//        String signValue = VnPayConfig.hmacSHA512(secretKey, rawData); // Hash bằng key
//        if (signValue.equals(vnp_SecureHash)) {
//            
//            // Giao dịch thành công
//            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
//                String orderIdStr = request.getParameter("vnp_TxnRef");
//                Long orderId = Long.parseLong(orderIdStr);
//                
//                // Cập nhật trạng thái đơn hàng
//                Order order = orderRepository.findById(orderId)
//                        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
//                
//                // Chỉ update nếu chưa thanh toán (tránh trùng lặp)
//                if(order.getStatus() == OrderStatus.PENDING) {
//                    order.setStatus(OrderStatus.SHIPPING); // Hoặc PAID tùy logic của bạn
//                    orderRepository.save(order);
//                }
//
//                return PaymentCallbackResponse.builder()
//                        .status("00")
//                        .message("Thanh toán thành công")
//                        .orderId(orderIdStr)
//                        .amount(request.getParameter("vnp_Amount"))
//                        .paymentDate(request.getParameter("vnp_PayDate"))
//                        .build();
//            } else {
//                return PaymentCallbackResponse.builder()
//                        .status(request.getParameter("vnp_ResponseCode"))
//                        .message("Thanh toán thất bại")
//                        .build();
//            }
//        } else {
//            throw new BadRequestException("Chữ ký không hợp lệ!");
//        }
//    }
    
    /**
     * Xử lý kết quả trả về (Callback)
     */
    @Override
    @Transactional // Đảm bảo cập nhật DB an toàn
    public PaymentCallbackResponse processVnPayCallback(HttpServletRequest request) {
        // 1. Lấy tất cả tham số VNPay gửi về
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        // 2. Lấy chữ ký (signature) do VNPay gửi kèm
        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        
        // 3. Xóa 2 tham số hash type và secure hash ra khỏi map trước khi tính toán lại checksum
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // 4. Tự tính toán lại checksum từ dữ liệu nhận được
        String signValue = hashAllFields(fields);

        // 5. So sánh checksum của mình tính với checksum của VNPay
        if (signValue.equals(vnp_SecureHash)) {
            // --- CHỮ KÝ HỢP LỆ (Dữ liệu toàn vẹn) ---
            
            String orderId = request.getParameter("vnp_TxnRef");
            String amount = request.getParameter("vnp_Amount");
            String responseCode = request.getParameter("vnp_ResponseCode"); // 00 = Thành công
            
            if ("00".equals(responseCode)) {
                // --- THANH TOÁN THÀNH CÔNG ---
                log.info("Thanh toán thành công cho đơn hàng ID: {}", orderId);

                // Cập nhật trạng thái đơn hàng
                Long id = Long.parseLong(orderId);
                Order order = orderRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));
                
                String userEmail = order.getCustomer().getUser().getEmail();
                notificationService.sendNotificationToUser(userEmail, "Thanh toán thành công cho đơn hàng #" + orderId);
                
                // Logic nghiệp vụ: Chỉ cập nhật nếu đang PENDING
                if (order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.PROCESSING); // Hoặc PAID
                    orderRepository.save(order);
                    log.info("Đã cập nhật trạng thái đơn hàng {} sang PROCESSING", id);
                }
                
                return PaymentCallbackResponse.builder()
                        .status("00")
                        .message("Thanh toán thành công")
                        .orderId(orderId)
                        .amount(amount)
                        .build();
            } else {
                // --- THANH TOÁN THẤT BẠI (User hủy, hết tiền...) ---
                log.warn("Thanh toán thất bại cho đơn hàng ID: {}. Mã lỗi: {}", orderId, responseCode);
                return PaymentCallbackResponse.builder()
                        .status(responseCode)
                        .message("Thanh toán thất bại")
                        .orderId(orderId)
                        .build();
            }
        } else {
            // --- CHỮ KÝ KHÔNG HỢP LỆ (Có thể là hacker giả mạo) ---
            log.error("Checksum không hợp lệ!");
            return PaymentCallbackResponse.builder()
                    .status("99")
                    .message("Chữ ký không hợp lệ")
                    .build();
        }
    }

    // Hàm helper để băm dữ liệu (giống hàm trong createPayment)
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