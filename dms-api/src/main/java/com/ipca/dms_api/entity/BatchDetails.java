package com.ipca.dms_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DMS_BATCH_DETAILS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(BatchDetailsId.class)
public class BatchDetails {

    @Id
    @Column(name = "COMPANY_ID", length = 2, nullable = false)
    private String companyId;

    @Id
    @Column(name = "LOCATION_ID", length = 3, nullable = false)
    private String locationId;

    @Id
    @Column(name = "SUB_APPLICATION_NAME", length = 50, nullable = false)
    private String subApplicationName; // COA | INVOICE | IMAGE

    @Id
    @Column(name = "BATCH_NUMBER", length = 30, nullable = false)
    private String batchNumber;

    @Id
    @Column(name = "PRODUCT_CODE", length = 10, nullable = false)
    private String productCode;

    @Column(name = "DIVISION_NAME", length = 20)
    private String divisionName;

    @Column(name = "APPLICATION_NAME", length = 35)
    private String applicationName;

    @Column(name = "TYPE", length = 15)
    private String type; // Third Party | Own | Loan License

    @Column(name = "PRODUCT_NAME", length = 100)
    private String productName;

    @Column(name = "VENDOR_CODE", length = 10)
    private String vendorCode;

    @Column(name = "VENDOR_NAME", length = 100)
    private String vendorName;

    @Column(name = "FILE_NAME", length = 150)
    private String fileName;

    @Column(name = "FILE_PATH", length = 300)
    private String filePath;

    @Column(name = "CREATED_BY", length = 30)
    private String createdBy;

    @Column(name = "CREATED_ON")
    private LocalDateTime createdOn;
}