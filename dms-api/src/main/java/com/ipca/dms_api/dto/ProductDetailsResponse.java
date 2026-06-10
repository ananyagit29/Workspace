package com.ipca.dms_api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailsResponse {
    private String productCode;
    private String productName;
    private String vendorCode;
    private String vendorName;
}