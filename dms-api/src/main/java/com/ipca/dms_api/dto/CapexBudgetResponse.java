package com.ipca.dms_api.dto;

import java.time.LocalDateTime;

public class CapexBudgetResponse {
    private String budgetCode;
    private String budgetType;
    private String fileName;
    private String filePath;
    private String companyId;
    private String locationId;
    private String divisionName;
    private String applicationName;
    private String createdBy;
    private LocalDateTime createdOn;
    private LocalDateTime docDate;
    private Integer revisionNo;
    private Boolean isLatestRevision;

    public CapexBudgetResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String budgetCode;
        private String budgetType;
        private String fileName;
        private String filePath;
        private String companyId;
        private String locationId;
        private String divisionName;
        private String applicationName;
        private String createdBy;
        private LocalDateTime createdOn;
        private LocalDateTime docDate;
        private Integer revisionNo;
        private Boolean isLatestRevision;

        public Builder budgetCode(String budgetCode) { this.budgetCode = budgetCode; return this; }
        public Builder budgetType(String budgetType) { this.budgetType = budgetType; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder companyId(String companyId) { this.companyId = companyId; return this; }
        public Builder locationId(String locationId) { this.locationId = locationId; return this; }
        public Builder divisionName(String divisionName) { this.divisionName = divisionName; return this; }
        public Builder applicationName(String applicationName) { this.applicationName = applicationName; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdOn(LocalDateTime createdOn) { this.createdOn = createdOn; return this; }
        public Builder docDate(LocalDateTime docDate) { this.docDate = docDate; return this; }
        public Builder revisionNo(Integer revisionNo) { this.revisionNo = revisionNo; return this; }
        public Builder isLatestRevision(Boolean isLatestRevision) { this.isLatestRevision = isLatestRevision; return this; }

        public CapexBudgetResponse build() {
            CapexBudgetResponse resp = new CapexBudgetResponse();
            resp.budgetCode = this.budgetCode;
            resp.budgetType = this.budgetType;
            resp.fileName = this.fileName;
            resp.filePath = this.filePath;
            resp.companyId = this.companyId;
            resp.locationId = this.locationId;
            resp.divisionName = this.divisionName;
            resp.applicationName = this.applicationName;
            resp.createdBy = this.createdBy;
            resp.createdOn = this.createdOn;
            resp.docDate = this.docDate;
            resp.revisionNo = this.revisionNo;
            resp.isLatestRevision = this.isLatestRevision;
            return resp;
        }
    }

    public String getBudgetCode() { return budgetCode; }
    public void setBudgetCode(String budgetCode) { this.budgetCode = budgetCode; }

    public String getBudgetType() { return budgetType; }
    public void setBudgetType(String budgetType) { this.budgetType = budgetType; }

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

    public LocalDateTime getDocDate() { return docDate; }
    public void setDocDate(LocalDateTime docDate) { this.docDate = docDate; }

    public Integer getRevisionNo() { return revisionNo; }
    public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }

    public Boolean getIsLatestRevision() { return isLatestRevision; }
    public void setIsLatestRevision(Boolean isLatestRevision) { this.isLatestRevision = isLatestRevision; }
}
