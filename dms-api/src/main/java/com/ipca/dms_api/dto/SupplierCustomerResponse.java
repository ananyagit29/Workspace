package com.ipca.dms_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierCustomerResponse {

    private String accountType;
    private String accountCode;
    private String accountName;
    private String companyId;
    private String locationId;
    private String divisionName;
    private String applicationName;
    private String fileName;
    private String filePath;
    private String createdBy;
    private LocalDateTime createdOn;

}
