package com.ipca.dms_api.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "DMS_USERS")
public class User {

    @Id
    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "EMAIL_ID")
    private String emailId;

    @Column(name = "EMPLOYEE_ID")
    private String employeeId;

    @Column(name = "LOCATION_NAME")
    private String locationName;

    @Column(name = "DEPARTMENT_NAME")
    private String departmentName;

    @Column(name = "ACCOUNT_STATUS")
    private String accountStatus;

    @Column(name = "LOGIN_STATUS")
    private String loginStatus;

    @Column(name = "BAD_LOGIN_COUNT")
    private Integer badLoginCount;

    @Column(name = "AUTH_TYPE")
    private String authType;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "USER_CREATED_DATE")
    private LocalDateTime userCreatedDate;

    @Column(name = "LAST_LOGIN")
    private LocalDateTime lastLogin;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(String loginStatus) {
        this.loginStatus = loginStatus;
    }

    public Integer getBadLoginCount() {
        return badLoginCount;
    }

    public void setBadLoginCount(Integer badLoginCount) {
        this.badLoginCount = badLoginCount;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getUserCreatedDate() {
        return userCreatedDate;
    }

    public void setUserCreatedDate(LocalDateTime userCreatedDate) {
        this.userCreatedDate = userCreatedDate;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

}