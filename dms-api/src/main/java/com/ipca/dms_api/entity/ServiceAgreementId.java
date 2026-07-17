package com.ipca.dms_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAgreementId implements Serializable {
    private String companyId;
    private String locationId;
    private String financialYear;
    private String subdivisionCode;
    private Integer docCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceAgreementId that = (ServiceAgreementId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(locationId, that.locationId) &&
               Objects.equals(financialYear, that.financialYear) &&
               Objects.equals(subdivisionCode, that.subdivisionCode) &&
               Objects.equals(docCode, that.docCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, locationId, financialYear, subdivisionCode, docCode);
    }
}
