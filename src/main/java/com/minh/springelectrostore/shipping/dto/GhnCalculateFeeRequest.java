package com.minh.springelectrostore.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GhnCalculateFeeRequest {
    @JsonProperty("service_type_id")
    private Integer serviceTypeId; // 2 = Chuẩn, 53320 = Tiết kiệm...

    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("length")
    private Integer length;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("weight")
    private Integer weight; // gram

    @JsonProperty("insurance_value")
    private Integer insuranceValue; // Giá trị đơn hàng (để tính bảo hiểm)
}