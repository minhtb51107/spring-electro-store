package com.minh.springelectrostore.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnFeeResponse {
    private Integer code; // 200 = Success
    private String message;
    private GhnFeeData data;

    @Data
    public static class GhnFeeData {
        @JsonProperty("total")
        private Integer total; // Tổng phí ship
        
        @JsonProperty("service_fee")
        private Integer serviceFee;
        
        @JsonProperty("insurance_fee")
        private Integer insuranceFee;
    }
}