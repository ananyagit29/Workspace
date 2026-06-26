package com.ipca.dms_api.dto;

import java.util.Date;

public class TruckLoadStuffResponse {

    private String invoiceNo;
    private String fileName;
    private String createdBy;
    private Date createdOn;
    private String filePath;

    public TruckLoadStuffResponse() {
    }

    public TruckLoadStuffResponse(String invoiceNo, String fileName, String createdBy, Date createdOn, String filePath) {
        this.invoiceNo = invoiceNo;
        this.fileName = fileName;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.filePath = filePath;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
