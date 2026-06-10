package com.ipca.dms_api.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceDocumentResponse {
    private Long id;
    private String invoiceNumber;
    private String fileName;
    private String filePath;
    private String companyId;
    private String locationId;
    private String divisionName;
    private String applicationName;
    private String createdBy;
    private LocalDateTime createdOn;
}
