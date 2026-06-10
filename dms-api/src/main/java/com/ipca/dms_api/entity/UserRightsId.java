package com.ipca.dms_api.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserRightsId implements Serializable {

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "COMPANY_ID")
    private String companyId;

    @Column(name = "DIVISION_NAME")
    private String divisionName;

    @Column(name = "LOCATION_ID")
    private String locationId;

    @Column(name = "APPLICATION_NAME")
    private String applicationName;

    @Column(name = "SUB_APPLICATION_NAME")
    private String subApplicationName;

    @Column(name = "MODULE")
    private String module;

    @Column(name = "ACCESS_TYPE")
    private String accessType;

    public UserRightsId() {}

    // constructor
    public UserRightsId(String userId, String companyId, String divisionName,
                        String locationId, String applicationName,
                        String subApplicationName, String module,
                        String accessType) {
        this.userId = userId;
        this.companyId = companyId;
        this.divisionName = divisionName;
        this.locationId = locationId;
        this.applicationName = applicationName;
        this.subApplicationName = subApplicationName;
        this.module = module;
        this.accessType = accessType;
    }

    // equals & hashCode (VERY IMPORTANT)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRightsId)) return false;
        UserRightsId that = (UserRightsId) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(companyId, that.companyId) &&
               Objects.equals(divisionName, that.divisionName) &&
               Objects.equals(locationId, that.locationId) &&
               Objects.equals(applicationName, that.applicationName) &&
               Objects.equals(subApplicationName, that.subApplicationName) &&
               Objects.equals(module, that.module) &&
               Objects.equals(accessType, that.accessType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, companyId, divisionName,
                            locationId, applicationName,
                            subApplicationName, module, accessType);
    }
}
