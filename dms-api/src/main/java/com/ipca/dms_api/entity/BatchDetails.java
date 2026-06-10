package com.ipca.dms_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DMS_BATCH_DETAILS")
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

    // Constructors
    public BatchDetails() {}

    public BatchDetails(String companyId, String locationId, String subApplicationName, 
                       String batchNumber, String productCode, String divisionName, 
                       String applicationName, String type, String productName, 
                       String vendorCode, String vendorName, String fileName, 
                       String filePath, String createdBy, LocalDateTime createdOn) {
        this.companyId = companyId;
        this.locationId = locationId;
        this.subApplicationName = subApplicationName;
        this.batchNumber = batchNumber;
        this.productCode = productCode;
        this.divisionName = divisionName;
        this.applicationName = applicationName;
        this.type = type;
        this.productName = productName;
        this.vendorCode = vendorCode;
        this.vendorName = vendorName;
        this.fileName = fileName;
        this.filePath = filePath;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
    }

    // Getters and Setters
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getSubApplicationName() { return subApplicationName; }
    public void setSubApplicationName(String subApplicationName) { this.subApplicationName = subApplicationName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getDivisionName() { return divisionName; }
    public void setDivisionName(String divisionName) { this.divisionName = divisionName; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String companyId;
        private String locationId;
        private String subApplicationName;
        private String batchNumber;
        private String productCode;
        private String divisionName;
        private String applicationName;
        private String type;
        private String productName;
        private String vendorCode;
        private String vendorName;
        private String fileName;
        private String filePath;
        private String createdBy;
        private LocalDateTime createdOn;

        public Builder companyId(String companyId) { this.companyId = companyId; return this; }
        public Builder locationId(String locationId) { this.locationId = locationId; return this; }
        public Builder subApplicationName(String subApplicationName) { this.subApplicationName = subApplicationName; return this; }
        public Builder batchNumber(String batchNumber) { this.batchNumber = batchNumber; return this; }
        public Builder productCode(String productCode) { this.productCode = productCode; return this; }
        public Builder divisionName(String divisionName) { this.divisionName = divisionName; return this; }
        public Builder applicationName(String applicationName) { this.applicationName = applicationName; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder vendorCode(String vendorCode) { this.vendorCode = vendorCode; return this; }
        public Builder vendorName(String vendorName) { this.vendorName = vendorName; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdOn(LocalDateTime createdOn) { this.createdOn = createdOn; return this; }

        public BatchDetails build() {
            return new BatchDetails(companyId, locationId, subApplicationName, batchNumber, 
                                   productCode, divisionName, applicationName, type, productName, 
                                   vendorCode, vendorName, fileName, filePath, createdBy, createdOn);
        }
    }
}