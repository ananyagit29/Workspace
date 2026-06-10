package com.ipca.dms_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "DMS_USER_RIGHTS")
public class UserRights {

    @EmbeddedId
    private UserRightsId id;

    @Column(name = "COMPANY_NAME")
    private String companyName;

    @Column(name = "LOCATION_NAME")
    private String locationName;

    public UserRights() {
    }

    // ===== Embedded ID Getter/Setter =====

    public UserRightsId getId() {
        return id;
    }

    public void setId(UserRightsId id) {
        this.id = id;
    }

    // ===== Normal Fields =====

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
