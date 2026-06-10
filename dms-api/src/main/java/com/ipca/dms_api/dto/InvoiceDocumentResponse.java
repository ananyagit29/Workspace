package com.ipca.dms_api.dto;

import java.time.LocalDateTime;

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

    public InvoiceDocumentResponse() {}

    public InvoiceDocumentResponse(Long id, String invoiceNumber, String fileName, String filePath,
                                  String companyId, String locationId, String divisionName,
                                  String applicationName, String createdBy, LocalDateTime createdOn) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.fileName = fileName;
        this.filePath = filePath;
        this.companyId = companyId;
        this.locationId = locationId;
        this.divisionName = divisionName;
        this.applicationName = applicationName;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getDivisionName() { return divisionName; }
    public void setDivisionName(String divisionName) { this.divisionName = divisionName; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
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

        public Builder id(Long id) { this.id = id; return this; }
        public Builder invoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder companyId(String companyId) { this.companyId = companyId; return this; }
        public Builder locationId(String locationId) { this.locationId = locationId; return this; }
        public Builder divisionName(String divisionName) { this.divisionName = divisionName; return this; }
        public Builder applicationName(String applicationName) { this.applicationName = applicationName; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdOn(LocalDateTime createdOn) { this.createdOn = createdOn; return this; }

        public InvoiceDocumentResponse build() {
            return new InvoiceDocumentResponse(id, invoiceNumber, fileName, filePath, companyId,
                                             locationId, divisionName, applicationName, createdBy, createdOn);
        }
    }
}
