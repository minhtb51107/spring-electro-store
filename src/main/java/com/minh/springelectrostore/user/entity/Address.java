package com.minh.springelectrostore.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotBlank
    @Column(nullable = false)
    private String receiverName; // Tên người nhận tại địa chỉ này (có thể khác tên User)

    @NotBlank
    @Column(nullable = false)
    // Regex đơn giản cho số điện thoại VN
    @Pattern(regexp = "(84|0[3|5|7|8|9])+([0-9]{8})\\b", message = "Số điện thoại không hợp lệ")
    private String receiverPhone;

    @NotBlank
    @Column(nullable = false)
    private String streetAddress; // Số nhà, tên đường

    @NotBlank
    @Column(nullable = false)
    private String ward; // Phường/Xã

    @NotBlank
    @Column(nullable = false)
    private String district; // Quận/Huyện

    @NotBlank
    @Column(nullable = false)
    private String province; // Tỉnh/Thành phố

    @Builder.Default
    @Column(name = "is_default")
    private boolean isDefault = false; // Địa chỉ mặc định
    
    @Column(name = "ghn_district_id")
    private Integer ghnDistrictId;

    @Column(name = "ghn_ward_code", length = 50)
    private String ghnWardCode;
}