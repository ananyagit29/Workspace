package com.ipca.dms_api.dto;

public class UserRightsDTO {

    private String userId;
    private String companyId;
    private String divisionName;
    private String locationId;
    private String applicationName;
    private String subApplicationName;
    private String module;
    private String accessType;
    private String companyName;
    private String locationName;

    public UserRightsDTO(String userId, String companyId, String divisionName,
                         String locationId, String applicationName, String subApplicationName,
                         String module, String accessType, String companyName, String locationName) {
        this.userId = userId;
        this.companyId = companyId;
        this.divisionName = divisionName;
        this.locationId = locationId;
        this.applicationName = applicationName;
        this.subApplicationName = subApplicationName;
        this.module = module;
        this.accessType = accessType;
        this.companyName = companyName;
        this.locationName = locationName;
    }

    // Getters and setters...

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getDivisionName() {
        return divisionName;
    }

    public void setDivisionName(String divisionName) {
        this.divisionName = divisionName;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getSubApplicationName() {
        return subApplicationName;
    }

    public void setSubApplicationName(String subApplicationName) {
        this.subApplicationName = subApplicationName;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
