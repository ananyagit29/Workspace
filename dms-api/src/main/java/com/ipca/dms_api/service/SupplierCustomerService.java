package com.ipca.dms_api.service;

import com.ipca.dms_api.dto.SupplierCustomerResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SupplierCustomerService {

    private final JdbcTemplate jdbcTemplate;

    public SupplierCustomerService(@Qualifier("primaryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
    }

    private String clean(String val) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : null;
    }

    public List<Map<String, Object>> getSearchAccountOptions(String accountType, String companyId, String locationId) {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT ACCOUNT_CODE as \"code\", ACCOUNT_NAME as \"name\" " +
                "FROM DMS_SUPPLIER_AND_CUSTOMER WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();

        if (clean(accountType) != null && !accountType.equalsIgnoreCase("SELECT")) {
            sql.append(" AND ACCOUNT_TYPE = ?");
            params.add(clean(accountType));
        }
        if (clean(companyId) != null) {
            sql.append(" AND COMPANY_ID = ?");
            params.add(clean(companyId));
        }
        if (clean(locationId) != null) {
            sql.append(" AND LOCATION_ID = ?");
            params.add(clean(locationId));
        }
        
        sql.append(" ORDER BY ACCOUNT_NAME");

        try {
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            System.err.println("Error fetching S&C account options: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public Page<SupplierCustomerResponse> searchDocuments(
            String accountType, String accountCode, String companyId, String locationId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (clean(accountType) != null && !accountType.equalsIgnoreCase("SELECT")) {
            where.append(" AND ACCOUNT_TYPE = ?");
            params.add(clean(accountType));
        }
        if (clean(accountCode) != null && !accountCode.equalsIgnoreCase("SELECT")) {
            where.append(" AND ACCOUNT_CODE = ?");
            params.add(clean(accountCode));
        }
        if (clean(companyId) != null) {
            where.append(" AND COMPANY_ID = ?");
            params.add(clean(companyId));
        }
        if (clean(locationId) != null) {
            where.append(" AND LOCATION_ID = ?");
            params.add(clean(locationId));
        }

        String countSql = "SELECT COUNT(*) FROM DMS_SUPPLIER_AND_CUSTOMER" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        List<Object> queryParams = new ArrayList<>(params);
        String dataSql = "SELECT ACCOUNT_TYPE, ACCOUNT_CODE, ACCOUNT_NAME, COMPANY_ID, LOCATION_ID, DIVISION_NAME, " +
                         "APPLICATION_NAME, FILE_NAME, FILE_PATH, CREATED_BY, CREATED_ON " +
                         "FROM DMS_SUPPLIER_AND_CUSTOMER " + where + " ORDER BY CREATED_ON DESC";

        dataSql += " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        queryParams.add(pageable.getOffset());
        queryParams.add(pageable.getPageSize());

        List<SupplierCustomerResponse> rows = jdbcTemplate.query(
            dataSql,
            (rs, rowNum) -> SupplierCustomerResponse.builder()
                .accountType(rs.getString("ACCOUNT_TYPE"))
                .accountCode(rs.getString("ACCOUNT_CODE"))
                .accountName(rs.getString("ACCOUNT_NAME"))
                .companyId(rs.getString("COMPANY_ID"))
                .locationId(rs.getString("LOCATION_ID"))
                .divisionName(rs.getString("DIVISION_NAME"))
                .applicationName(rs.getString("APPLICATION_NAME"))
                .fileName(rs.getString("FILE_NAME"))
                .filePath(rs.getString("FILE_PATH"))
                .createdBy(rs.getString("CREATED_BY"))
                .createdOn(rs.getTimestamp("CREATED_ON") != null ? rs.getTimestamp("CREATED_ON").toLocalDateTime() : null)
                .build(),
            queryParams.toArray());

        return new PageImpl<>(rows, pageable, total == null ? 0L : total);
    }

    public void removeDocument(String accountCode, String fileName) {
        String selectSql = "SELECT FILE_PATH FROM DMS_SUPPLIER_AND_CUSTOMER WHERE ACCOUNT_CODE = ? AND FILE_NAME = ?";
        try {
            List<String> paths = jdbcTemplate.queryForList(selectSql, String.class, accountCode, fileName);
            for (String filePath : paths) {
                if (filePath != null && !filePath.isEmpty()) {
                    try {
                        Files.deleteIfExists(Paths.get(filePath));
                    } catch (IOException e) {
                        System.err.println("Warning: Could not delete physical file: " + filePath);
                    }
                }
            }
            String deleteSql = "DELETE FROM DMS_SUPPLIER_AND_CUSTOMER WHERE ACCOUNT_CODE = ? AND FILE_NAME = ?";
            jdbcTemplate.update(deleteSql, accountCode, fileName);
        } catch (Exception e) {
            System.err.println("Error removing S&C document: " + e.getMessage());
            throw new RuntimeException("Failed to remove document");
        }
    }
}
