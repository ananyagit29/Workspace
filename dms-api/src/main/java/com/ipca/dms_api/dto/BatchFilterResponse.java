package com.ipca.dms_api.dto;

import java.util.List;

public class BatchFilterResponse {
    private List<String> vendorCodes;
    private List<String> productCodes;
    private List<String> batchNumbers;

    public BatchFilterResponse() {}

    public BatchFilterResponse(List<String> vendorCodes, List<String> productCodes, List<String> batchNumbers) {
        this.vendorCodes = vendorCodes;
        this.productCodes = productCodes;
        this.batchNumbers = batchNumbers;
    }

    // Getters and Setters
    public List<String> getVendorCodes() { return vendorCodes; }
    public void setVendorCodes(List<String> vendorCodes) { this.vendorCodes = vendorCodes; }

    public List<String> getProductCodes() { return productCodes; }
    public void setProductCodes(List<String> productCodes) { this.productCodes = productCodes; }

    public List<String> getBatchNumbers() { return batchNumbers; }
    public void setBatchNumbers(List<String> batchNumbers) { this.batchNumbers = batchNumbers; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<String> vendorCodes;
        private List<String> productCodes;
        private List<String> batchNumbers;

        public Builder vendorCodes(List<String> vendorCodes) { this.vendorCodes = vendorCodes; return this; }
        public Builder productCodes(List<String> productCodes) { this.productCodes = productCodes; return this; }
        public Builder batchNumbers(List<String> batchNumbers) { this.batchNumbers = batchNumbers; return this; }

        public BatchFilterResponse build() {
            return new BatchFilterResponse(vendorCodes, productCodes, batchNumbers);
        }
    }
}