package com.ipca.dms_api.service;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.annotation.PostConstruct;

@Service
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("hrmsDataSource")
    private DataSource hrmsDataSource;

    private JdbcTemplate hrmsJdbcTemplate;

    @PostConstruct
    public void init() {
        this.hrmsJdbcTemplate = new JdbcTemplate(java.util.Objects.requireNonNull(hrmsDataSource));
    }

    public Map<String, Object> checkUser(@RequestParam String uid) {
        try {
            String dmsSql = "SELECT USER_ID FROM DMS_USERS WHERE USER_ID = ?";
            List<Map<String, Object>> dmsUsers = jdbcTemplate.queryForList(dmsSql, uid);
            if (!dmsUsers.isEmpty()) {
                return Map.of("status", "EXISTS");
            }

            String hrmsSql = """
                    SELECT FIRST_NAME, LAST_NAME, EMPLOYEE_ID, OFFICIAL_EMAIL_ID, LOCATION_NAME, DEPARTMENT
                    FROM EMPLOYEE_MASTER
                    WHERE DOMAIN_ID = ?
                    """;

            List<Map<String, Object>> hrmsUsers = hrmsJdbcTemplate.queryForList(hrmsSql, uid);

            if (!hrmsUsers.isEmpty()) {
                Map<String, Object> user = hrmsUsers.get(0);
                return Map.of(
                        "status", "FOUND_HRMS",
                        "firstName", user.get("FIRST_NAME"),
                        "lastName", user.get("LAST_NAME"),
                        "employeeId", user.get("EMPLOYEE_ID"),
                        "emailId", user.get("OFFICIAL_EMAIL_ID"),
                        "locationName", user.get("LOCATION_NAME"),
                        "departmentName", user.get("DEPARTMENT"));
            }

            return Map.of("status", "NOT_FOUND");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }
}
