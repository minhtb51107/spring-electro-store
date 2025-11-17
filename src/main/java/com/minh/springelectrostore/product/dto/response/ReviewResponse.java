package com.minh.springelectrostore.product.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReviewResponse {
    private Long id;
    private Integer rating;
    private String comment;
    private String userName; // Chỉ hiện tên user, không hiện email
    private String userAvatar; // (Nếu có)
    private OffsetDateTime createdAt;
    private List<String> images;
}