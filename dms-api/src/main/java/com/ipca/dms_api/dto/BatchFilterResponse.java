package com.ipca.dms_api.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchFilterResponse {
    private List<String> vendorCodes;
    private List<String> productCodes;
    private List<String> batchNumbers;
}