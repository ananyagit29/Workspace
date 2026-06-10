package com.ipca.dms_api.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSearchResponse {
    private String type;
    private String productCode;
    private String productName;
    private String vendorCode;
    private String vendorName;
    private String batchNumber;
    private String subApplicationName;
    private String fileName;
    private String filePath;
    private String createdBy;
    private LocalDateTime createdOn;
}