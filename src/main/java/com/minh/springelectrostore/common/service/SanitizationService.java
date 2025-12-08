package com.minh.springelectrostore.common.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class SanitizationService {

    /**
     * Chế độ nghiêm ngặt: Loại bỏ TẤT CẢ thẻ HTML.
     * Dùng cho: Tên, Tiêu đề, Comment đơn giản.
     * Ví dụ: "Hello <script>alert(1)</script>" -> "Hello "
     */
    public String sanitize(String content) {
        if (content == null) {
            return null;
        }
        return Jsoup.clean(content, Safelist.none());
    }

    /**
     * Chế độ cơ bản: Giữ lại các thẻ định dạng văn bản (b, i, u, p, br...).
     * Dùng cho: Bài viết blog, Mô tả sản phẩm (nếu cho phép rich text).
     */
    public String sanitizeBasic(String content) {
        if (content == null) {
            return null;
        }
        return Jsoup.clean(content, Safelist.basic());
    }
}