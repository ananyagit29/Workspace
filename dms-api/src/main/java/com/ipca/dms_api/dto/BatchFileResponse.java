package com.ipca.dms_api.dto;

public class BatchFileResponse {
    private String subApplicationName; // IMAGE | COA | INVOICE
    private String fileName;
    private String filePath;
    private boolean hasFile; // true = show button, false = show upload input

    public BatchFileResponse() {}

    public BatchFileResponse(String subApplicationName, String fileName, String filePath, boolean hasFile) {
        this.subApplicationName = subApplicationName;
        this.fileName = fileName;
        this.filePath = filePath;
        this.hasFile = hasFile;
    }

    // Getters and Setters
    public String getSubApplicationName() { return subApplicationName; }
    public void setSubApplicationName(String subApplicationName) { this.subApplicationName = subApplicationName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public boolean isHasFile() { return hasFile; }
    public void setHasFile(boolean hasFile) { this.hasFile = hasFile; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String subApplicationName;
        private String fileName;
        private String filePath;
        private boolean hasFile;

        public Builder subApplicationName(String subApplicationName) { this.subApplicationName = subApplicationName; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder hasFile(boolean hasFile) { this.hasFile = hasFile; return this; }

        public BatchFileResponse build() {
            return new BatchFileResponse(subApplicationName, fileName, filePath, hasFile);
        }
    }
}