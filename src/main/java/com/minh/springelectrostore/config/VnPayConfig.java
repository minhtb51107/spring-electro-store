package com.minh.springelectrostore.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
public class VnPayConfig {
	
	@org.springframework.beans.factory.annotation.Value("${vnpay.hash-secret}")
    private String secretKey; // Inject secret key vào đây (cần sửa class thành @Component hoặc truy cập static khéo léo)

    // Hàm tiện ích: Lấy IP của người dùng gửi request
    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }

    // Hàm tiện ích: Tạo chuỗi ngẫu nhiên (cho mã giao dịch)
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Hàm quan trọng nhất: Mã hóa dữ liệu bằng thuật toán HMAC SHA512
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception ex) {
            return "";
        }
    }
    
 // Dưới đây là logic sắp xếp và nối chuỗi (bạn có thể copy vào PaymentServiceImpl hoặc VnPayConfig)
    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        // LƯU Ý: Bạn cần truyền vnp_HashSecret vào hàm hmacSHA512
        // Ở đây tôi giả sử bạn sẽ gọi hmacSHA512 từ Service và truyền key vào.
        return sb.toString(); 
    }
}