package com.minh.springelectrostore.product.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.util.List;

@Data
public class UpdateReviewRequest {
    @Min(1) @Max(5)
    private Integer rating; // Có thể null nếu chỉ sửa comment
    
    private String comment;
    
    // Nếu gửi list mới, ta sẽ thay thế list cũ (hoặc thêm logic merge tùy bạn)
    private List<String> imageUrls; 
}