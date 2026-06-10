package com.ipca.dms_api.dto;

import java.time.LocalDateTime;

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

    public BatchSearchResponse() {}

    public BatchSearchResponse(String type, String productCode, String productName, String vendorCode,
                             String vendorName, String batchNumber, String subApplicationName,
                             String fileName, String filePath, String createdBy, LocalDateTime createdOn) {
        this.type = type;
        this.productCode = productCode;
        this.productName = productName;
        this.vendorCode = vendorCode;
        this.vendorName = vendorName;
        this.batchNumber = batchNumber;
        this.subApplicationName = subApplicationName;
        this.fileName = fileName;
        this.filePath = filePath;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getSubApplicationName() { return subApplicationName; }
    public void setSubApplicationName(String subApplicationName) { this.subApplicationName = subApplicationName; }

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

        public Builder type(String type) { this.type = type; return this; }
        public Builder productCode(String productCode) { this.productCode = productCode; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder vendorCode(String vendorCode) { this.vendorCode = vendorCode; return this; }
        public Builder vendorName(String vendorName) { this.vendorName = vendorName; return this; }
        public Builder batchNumber(String batchNumber) { this.batchNumber = batchNumber; return this; }
        public Builder subApplicationName(String subApplicationName) { this.subApplicationName = subApplicationName; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdOn(LocalDateTime createdOn) { this.createdOn = createdOn; return this; }

        public BatchSearchResponse build() {
            return new BatchSearchResponse(type, productCode, productName, vendorCode, vendorName,
                                         batchNumber, subApplicationName, fileName, filePath, createdBy, createdOn);
        }
    }
}