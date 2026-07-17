package com.ipca.dms_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "DMS_SERVICE_AGREEMENT")
@IdClass(ServiceAgreementId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceAgreementEntity {

    @Id
    @Column(name = "COMPANY_ID", length = 2, nullable = false)
    private String companyId;

    @Id
    @Column(name = "LOCATION_ID", length = 3, nullable = false)
    private String locationId;

    @Id
    @Column(name = "FINANCIAL_YEAR", length = 9, nullable = false)
    private String financialYear;

    @Id
    @Column(name = "SUBDIVISION_CODE", length = 3, nullable = false)
    private String subdivisionCode;

    @Id
    @Column(name = "DOC_CODE", nullable = false)
    private Integer docCode;

    @Column(name = "DIVISION_NAME", length = 20)
    private String divisionName;

    @Column(name = "APPLICATION_NAME", length = 20)
    private String applicationName;

    @Column(name = "SUBDIVISION_NAME", length = 30)
    private String subdivisionName;

    @Column(name = "DOCTOR_PAN", length = 10)
    private String doctorPan;

    @Column(name = "DOCTOR_CODE", length = 10)
    private String doctorCode;

    @Column(name = "DOCTOR_NAME", length = 100)
    private String doctorName;

    @Column(name = "IN_FAVOUR_OF", length = 100)
    private String inFavourOf;

    @Column(name = "EMPLOYEE_ID", length = 10)
    private String employeeId;

    @Column(name = "EMPLOYEE_NAME", length = 100)
    private String employeeName;

    @Column(name = "RC_CODE", length = 10)
    private String rcCode;

    @Column(name = "INTERFACE_APP_NO", length = 20)
    private String interfaceAppNo;

    @Column(name = "CME_LOG_NO", length = 20)
    private String cmeLogNo;

    @Column(name = "AMOUNT")
    private Long amount; // NUMBER(13,0)

    @Column(name = "EVENT_FROM_DATE")
    private Date eventFromDate;

    @Column(name = "EVENT_TO_DATE")
    private Date eventToDate;

    @Column(name = "EVENT_NAME", length = 60)
    private String eventName;

    @Column(name = "VOUCHER_NO", length = 10)
    private String voucherNo;

    @Column(name = "VOUCHER_DATE")
    private Date voucherDate;

    @Column(name = "CHEQUE_NO", length = 10)
    private String chequeNo;

    @Column(name = "CHEQUE_DATE")
    private Date chequeDate;

    @Column(name = "CREATED_BY", length = 30)
    private String createdBy;

    @Column(name = "CREATED_ON")
    private LocalDateTime createdOn;

    @Column(name = "FILE_NAME", length = 100)
    private String fileName;

    @Column(name = "FILE_PATH", length = 300)
    private String filePath;
}
