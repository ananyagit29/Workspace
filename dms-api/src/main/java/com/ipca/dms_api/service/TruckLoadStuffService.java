package com.ipca.dms_api.service;

import com.ipca.dms_api.dto.TruckLoadStuffResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TruckLoadStuffService {

    private final JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Value("${dms.upload.directory}")
    private String uploadDir;

    public TruckLoadStuffService(@Qualifier("primaryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
    }

    private String clean(String val) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : null;
    }

    public List<String> getSearchOptions(String companyId, String locationId, String year) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT INVOICE_NO FROM DMS_TRUCK_LOAD_STUFF WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (clean(year) != null) {
            sql.append(" AND FINANCIAL_YEAR = ?");
            params.add(clean(year));
        }
        if (clean(companyId) != null) {
            sql.append(" AND COMPANY_ID = ?");
            params.add(clean(companyId));
        }
        if (clean(locationId) != null) {
            sql.append(" AND LOCATION_ID = ?");
            params.add(clean(locationId));
        }

        sql.append(" ORDER BY INVOICE_NO");

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            return results.stream()
                    .map(row -> (String) row.get("INVOICE_NO"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching TLS search options: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public List<TruckLoadStuffResponse> searchDocuments(String invoiceNo, String companyId, String locationId, String year) {
        StringBuilder sql = new StringBuilder(
                "SELECT INVOICE_NO, FILE_NAME, CREATED_BY, CREATED_ON, FILE_PATH " +
                "FROM DMS_TRUCK_LOAD_STUFF WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (clean(year) != null) {
            sql.append(" AND FINANCIAL_YEAR = ?");
            params.add(clean(year));
        }
        if (clean(companyId) != null) {
            sql.append(" AND COMPANY_ID = ?");
            params.add(clean(companyId));
        }
        if (clean(locationId) != null) {
            sql.append(" AND LOCATION_ID = ?");
            params.add(clean(locationId));
        }
        if (clean(invoiceNo) != null) {
            sql.append(" AND INVOICE_NO = ?");
            params.add(clean(invoiceNo));
        }

        sql.append(" ORDER BY CREATED_ON DESC");

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            List<TruckLoadStuffResponse> responses = new ArrayList<>();
            for (Map<String, Object> row : results) {
                TruckLoadStuffResponse res = new TruckLoadStuffResponse();
                res.setInvoiceNo((String) row.get("INVOICE_NO"));
                res.setFileName((String) row.get("FILE_NAME"));
                res.setCreatedBy((String) row.get("CREATED_BY"));
                res.setCreatedOn((Date) row.get("CREATED_ON"));
                res.setFilePath((String) row.get("FILE_PATH"));
                responses.add(res);
            }
            return responses;
        } catch (Exception e) {
            System.err.println("Error searching TLS documents: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public File getFile(String invoiceNo, String fileName) throws IOException {
        String sql = "SELECT FILE_PATH FROM DMS_TRUCK_LOAD_STUFF WHERE INVOICE_NO = ? AND FILE_NAME = ?";
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, invoiceNo, fileName);
            if (!results.isEmpty()) {
                String path = (String) results.get(0).get("FILE_PATH");
                if (path != null && !path.trim().isEmpty()) {
                    File file = new File(path);
                    if (file.exists()) return file;
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving file path from DB: " + e.getMessage());
        }

        // Fallback
        java.nio.file.Path filePath = Paths.get(uploadDir, "TRUCK_LOAD_STUFF", invoiceNo, fileName);
        return filePath.toFile();
    }

    public void removeDocument(String invoiceNo, String fileName) {
        try {
            File f = getFile(invoiceNo, fileName);
            if (f != null && f.exists()) {
                Files.deleteIfExists(f.toPath());
            }
            String sql = "DELETE FROM DMS_TRUCK_LOAD_STUFF WHERE INVOICE_NO = ? AND FILE_NAME = ?";
            jdbcTemplate.update(sql, invoiceNo, fileName);
        } catch (Exception e) {
            System.err.println("Error removing TLS document: " + e.getMessage());
        }
    }
}
