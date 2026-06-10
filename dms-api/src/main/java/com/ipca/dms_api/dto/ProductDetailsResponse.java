package com.ipca.dms_api.dto;

public class ProductDetailsResponse {
    private String productCode;
    private String productName;
    private String vendorCode;
    private String vendorName;

    public ProductDetailsResponse() {}

    public ProductDetailsResponse(String productCode, String productName, String vendorCode, String vendorName) {
        this.productCode = productCode;
        this.productName = productName;
        this.vendorCode = vendorCode;
        this.vendorName = vendorName;
    }

    // Getters and Setters
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String productCode;
        private String productName;
        private String vendorCode;
        private String vendorName;

        public Builder productCode(String productCode) { this.productCode = productCode; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder vendorCode(String vendorCode) { this.vendorCode = vendorCode; return this; }
        public Builder vendorName(String vendorName) { this.vendorName = vendorName; return this; }

        public ProductDetailsResponse build() {
            return new ProductDetailsResponse(productCode, productName, vendorCode, vendorName);
        }
    }
}