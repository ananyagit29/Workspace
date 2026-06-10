package com.ipca.dms_api.entity;

import java.io.Serializable;
import java.util.Objects;

public class BatchDetailsId implements Serializable {

    private String companyId;
    private String locationId;
    private String subApplicationName;
    private String batchNumber;
    private String productCode;

    public BatchDetailsId() {}

    public BatchDetailsId(String companyId, String locationId, String subApplicationName,
                        String batchNumber, String productCode) {
        this.companyId = companyId;
        this.locationId = locationId;
        this.subApplicationName = subApplicationName;
        this.batchNumber = batchNumber;
        this.productCode = productCode;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchDetailsId that = (BatchDetailsId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(locationId, that.locationId) &&
               Objects.equals(subApplicationName, that.subApplicationName) &&
               Objects.equals(batchNumber, that.batchNumber) &&
               Objects.equals(productCode, that.productCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, locationId, subApplicationName, batchNumber, productCode);
    }
}