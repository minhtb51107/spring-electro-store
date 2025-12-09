package com.minh.springelectrostore.modules.user.entity;

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
    private String receiverName;

    @NotBlank
    @Column(nullable = false)
    @Pattern(regexp = "(84|0[3|5|7|8|9])+([0-9]{8})\\b", message = "Số điện thoại không hợp lệ")
    private String receiverPhone;

    @NotBlank
    @Column(nullable = false)
    private String streetAddress;

    @NotBlank
    @Column(nullable = false)
    private String ward;

    @NotBlank
    @Column(nullable = false)
    private String district;

    @NotBlank
    @Column(nullable = false)
    private String province;

    @Builder.Default
    @Column(name = "is_default")
    private boolean isDefault = false;
    
    @Column(name = "ghn_province_id")
    private Integer ghnProvinceId;

    @Column(name = "ghn_district_id")
    private Integer ghnDistrictId;

    @Column(name = "ghn_ward_code", length = 50)
    private String ghnWardCode;
}