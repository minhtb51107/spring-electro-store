package com.minh.springelectrostore.shared.util;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SlugService {

    // Biểu thức chính quy (Regex) để loại bỏ các dấu (accents)
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    // Biểu thức chính quy cho các khoảng trắng
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    // Biểu thức chính quy cho các dấu gạch ngang lặp lại
    private static final Pattern EDGES_HYPHENS = Pattern.compile("(^-|-$|(?<=-)-)");

    /**
     * Chuyển đổi một chuỗi bất kỳ thành một "slug" (URL-friendly string).
     * Ví dụ: "Laptop Gaming (Mới 2025!)" -> "laptop-gaming-moi-2025"
     *
     * @param input Chuỗi đầu vào (tên danh mục, tên sản phẩm...)
     * @return Chuỗi slug đã được chuẩn hóa.
     */
    public String toSlug(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input string cannot be null for slug generation");
        }

        // 1. Chuẩn hóa chuỗi (loại bỏ dấu tiếng Việt)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = NONLATIN.matcher(normalized).replaceAll("");

        // 2. Thay thế khoảng trắng bằng gạch ngang, chuyển sang chữ thường
        String slug = WHITESPACE.matcher(noAccents).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);

        // 3. Loại bỏ các gạch ngang thừa (ví dụ: "laptop--gaming" -> "laptop-gaming")
        slug = EDGES_HYPHENS.matcher(slug).replaceAll("");

        return slug;
    }
}