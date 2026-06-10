package com.ipca.dms_api.entity;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BatchDetailsId implements Serializable {

    private String companyId;
    private String locationId;
    private String subApplicationName;
    private String batchNumber;
    private String productCode;
}