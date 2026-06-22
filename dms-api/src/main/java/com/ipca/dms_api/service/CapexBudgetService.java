package com.ipca.dms_api.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ipca.dms_api.dto.CapexBudgetResponse;

@Service
public class CapexBudgetService {

    private static final long MAX_FILE_SIZE = 1024 * 1024;
    private final JdbcTemplate jdbcTemplate;
    private final String uploadDir = "d:/Workspace/Docs/Capex";

    public CapexBudgetService(@Qualifier("primaryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(java.util.Objects.requireNonNull(dataSource));
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String clean(String val) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : null;
    }

    public List<String> getBudgetTypes(String companyId, String locationId, String year) {
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT CAPEX_TYPE FROM FA_CAPEX_BUDGET@IPCASCMDRDB WHERE CAPEX_TYPE IS NOT NULL");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (companyId != null && !companyId.isEmpty()) {
            sql.append(" AND COMPANY_CODE = ?");
            params.add(companyId);
        }
        if (locationId != null && !locationId.isEmpty()) {
            sql.append(" AND ENTITY_CODE = ?");
            params.add(locationId);
        }
        if (false && year != null && year.matches("\\d{4}-\\d{4}")) {
            String[] parts = year.split("-");
            int y1 = Integer.parseInt(parts[0]);
            int y2 = Integer.parseInt(parts[1]);
            sql.append(" AND DOC_DATE BETWEEN ? AND ?");
            params.add(java.sql.Date.valueOf(y1 + "-04-01"));
            params.add(java.sql.Date.valueOf(y2 + "-03-31"));
        }
        sql.append(" ORDER BY CAPEX_TYPE");

        try {
            return jdbcTemplate.queryForList(sql.toString(), String.class, params.toArray());
        } catch (Exception e) {
            System.err.println("Error fetching budget types: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public List<String> getBudgetCodes(String budgetType, String companyId, String locationId, String year) {
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT DOC_CODE FROM FA_CAPEX_BUDGET@IPCASCMDRDB WHERE DOC_CODE IS NOT NULL");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (budgetType != null && !budgetType.isEmpty()) {
            sql.append(" AND CAPEX_TYPE = ?");
            params.add(budgetType);
        }
        if (companyId != null && !companyId.isEmpty()) {
            sql.append(" AND COMPANY_CODE = ?");
            params.add(companyId);
        }
        if (locationId != null && !locationId.isEmpty()) {
            sql.append(" AND ENTITY_CODE = ?");
            params.add(locationId);
        }
        if (false && year != null && year.matches("\\d{4}-\\d{4}")) {
            String[] parts = year.split("-");
            int y1 = Integer.parseInt(parts[0]);
            int y2 = Integer.parseInt(parts[1]);
            sql.append(" AND DOC_DATE BETWEEN ? AND ?");
            params.add(java.sql.Date.valueOf(y1 + "-04-01"));
            params.add(java.sql.Date.valueOf(y2 + "-03-31"));
        }
        sql.append(" ORDER BY DOC_CODE");

        try {
            return jdbcTemplate.queryForList(sql.toString(), String.class, params.toArray());
        } catch (Exception e) {
            System.err.println("Error fetching budget codes: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    private String formatYearForDb(String year) {
        String cleaned = clean(year);
        if (cleaned != null && cleaned.matches("\\d{4}-\\d{4}")) {
            return cleaned.substring(0, 4) + cleaned.substring(7);
        }
        return cleaned;
    }

    public List<String> getSearchBudgetCodes(String budgetType, String companyId, String locationId, String year) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT BUDGET_CODE FROM DMS_CAPEX_BUDGET WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (clean(budgetType) != null) {
            sql.append(" AND BUDGET_TYPE = ?");
            params.add(clean(budgetType));
        }
        if (clean(companyId) != null) {
            sql.append(" AND COMPANY_ID = ?");
            params.add(clean(companyId));
        }
        if (clean(locationId) != null) {
            sql.append(" AND LOCATION_ID = ?");
            params.add(clean(locationId));
        }
        if (formatYearForDb(year) != null) {
            sql.append(" AND FINANCIAL_YEAR = ?");
            params.add(formatYearForDb(year));
        }
        sql.append(" ORDER BY BUDGET_CODE");

        try {
            return jdbcTemplate.queryForList(sql.toString(), String.class, params.toArray());
        } catch (Exception e) {
            System.err.println("Error fetching search budget codes: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public List<String> getRevisions(String budgetCode) {
        String sql = "SELECT DISTINCT REVISION_NO FROM DMS_CAPEX_BUDGET WHERE BUDGET_CODE = ? ORDER BY REVISION_NO DESC";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> String.valueOf(rs.getInt("REVISION_NO")), budgetCode);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    private boolean existsInScm(String budgetCode, String companyId, String locationId) {
        String sql = "SELECT COUNT(*) FROM FA_CAPEX_BUDGET@IPCASCMDRDB WHERE DOC_CODE = ? AND COMPANY_CODE = ? AND ENTITY_CODE = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, budgetCode, companyId, locationId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private CapexBudgetResponse findLocalByCode(String budgetCode) {
        String sql = "SELECT * FROM DMS_CAPEX_BUDGET WHERE BUDGET_CODE = ? ORDER BY REVISION_NO DESC FETCH NEXT 1 ROWS ONLY";
        try {
            List<CapexBudgetResponse> list = jdbcTemplate.query(sql, (rs, rowNum) -> CapexBudgetResponse.builder()
                .budgetCode(rs.getString("BUDGET_CODE"))
                .budgetType(rs.getString("BUDGET_TYPE"))
                .fileName(rs.getString("FILE_NAME"))
                .filePath(rs.getString("FILE_PATH"))
                .revisionNo(rs.getInt("REVISION_NO"))
                .build(), budgetCode);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private CapexBudgetResponse findLocalByCodeAndRevision(String budgetCode, int revision) {
        String sql = "SELECT * FROM DMS_CAPEX_BUDGET WHERE BUDGET_CODE = ? AND REVISION_NO = ?";
        try {
            List<CapexBudgetResponse> list = jdbcTemplate.query(sql, (rs, rowNum) -> CapexBudgetResponse.builder()
                .budgetCode(rs.getString("BUDGET_CODE"))
                .budgetType(rs.getString("BUDGET_TYPE"))
                .fileName(rs.getString("FILE_NAME"))
                .filePath(rs.getString("FILE_PATH"))
                .revisionNo(rs.getInt("REVISION_NO"))
                .build(), budgetCode, revision);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public CapexBudgetResponse saveCapex(
            String budgetType, String budgetCode, String companyId, String locationId,
            String divisionName, String applicationName, String financialYear, String userId,
            MultipartFile file) throws IOException {

        if (budgetCode == null || budgetCode.isBlank()) throw new IllegalArgumentException("Budget code required");
        if (budgetType == null || budgetType.isBlank()) throw new IllegalArgumentException("Budget type required");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File required");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File size must be 1 MB or less.");
        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files allowed.");
        }

        if (!existsInScm(budgetCode, companyId, locationId)) {
            throw new IllegalArgumentException("Budget Code does not exist in SCM ERP.");
        }

        if (findLocalByCode(budgetCode) != null) {
            throw new IllegalArgumentException("Budget Code already exists in DMS.");
        }

        String safeFileName = budgetCode.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
        Path fullPath = Paths.get(uploadDir, safeFileName);

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO DMS_CAPEX_BUDGET
                (COMPANY_ID, LOCATION_ID, DIVISION_NAME, APPLICATION_NAME, FINANCIAL_YEAR, BUDGET_TYPE, BUDGET_CODE, REVISION_NO, CREATED_BY, CREATED_ON, FILE_NAME, FILE_PATH)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                companyId, locationId, divisionName, applicationName, formatYearForDb(financialYear), budgetType, budgetCode, 0, userId, Timestamp.valueOf(now), safeFileName, fullPath.toString());

        file.transferTo(fullPath.toFile());

        return CapexBudgetResponse.builder().budgetCode(budgetCode).fileName(safeFileName).build();
    }

    public CapexBudgetResponse reviseCapex(String budgetCode, String userId, MultipartFile file) throws IOException {
        CapexBudgetResponse existing = findLocalByCode(budgetCode);
        if (existing == null) throw new IllegalArgumentException("Budget Code not found in DMS.");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File required");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File size must be 1 MB or less.");

        int newRevision = existing.getRevisionNo() + 1;
        String safeFileName = budgetCode.replaceAll("[^a-zA-Z0-9.-]", "_") + "_rev" + newRevision + ".pdf";
        Path fullPath = Paths.get(uploadDir, safeFileName);

        LocalDateTime now = LocalDateTime.now();

        // Fetch the common fields from the existing row to avoid ORA parameter binding issues in INSERT ... SELECT
        java.util.Map<String, Object> oldRow = jdbcTemplate.queryForMap(
            "SELECT COMPANY_ID, LOCATION_ID, DIVISION_NAME, APPLICATION_NAME, FINANCIAL_YEAR, BUDGET_TYPE FROM DMS_CAPEX_BUDGET WHERE BUDGET_CODE = ? AND REVISION_NO = ?",
            budgetCode, existing.getRevisionNo()
        );

        jdbcTemplate.update("""
                INSERT INTO DMS_CAPEX_BUDGET
                (COMPANY_ID, LOCATION_ID, DIVISION_NAME, APPLICATION_NAME, FINANCIAL_YEAR, BUDGET_TYPE, BUDGET_CODE, REVISION_NO, DOC_DATE, CREATED_BY, CREATED_ON, FILE_NAME, FILE_PATH)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                oldRow.get("COMPANY_ID"), oldRow.get("LOCATION_ID"), oldRow.get("DIVISION_NAME"), oldRow.get("APPLICATION_NAME"), oldRow.get("FINANCIAL_YEAR"), oldRow.get("BUDGET_TYPE"), 
                budgetCode, newRevision, Timestamp.valueOf(now), userId, Timestamp.valueOf(now), safeFileName, fullPath.toString());

        file.transferTo(fullPath.toFile());

        return CapexBudgetResponse.builder().budgetCode(budgetCode).fileName(safeFileName).build();
    }

    public void removeCapex(String budgetCode, String revision) throws IOException {
        CapexBudgetResponse existing;
        if (revision != null) {
            existing = findLocalByCodeAndRevision(budgetCode, Integer.parseInt(revision));
        } else {
            existing = findLocalByCode(budgetCode);
        }
        
        if (existing != null) {
            if (revision != null) {
                jdbcTemplate.update("DELETE FROM DMS_CAPEX_BUDGET WHERE BUDGET_CODE = ? AND REVISION_NO = ?", budgetCode, Integer.parseInt(revision));
            } else {
                jdbcTemplate.update("DELETE FROM DMS_CAPEX_BUDGET WHERE BUDGET_CODE = ?", budgetCode);
            }
            Files.deleteIfExists(Paths.get(existing.getFilePath()));
        }
    }

    public Page<CapexBudgetResponse> search(
            String budgetType, String budgetCode, String revision, String companyId, String locationId, String year, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (clean(budgetCode) != null) {
            where.append(" AND BUDGET_CODE = ?");
            params.add(clean(budgetCode));
        }
        if (clean(budgetType) != null) {
            where.append(" AND BUDGET_TYPE = ?");
            params.add(clean(budgetType));
        }
        if (clean(locationId) != null) {
            where.append(" AND LOCATION_ID = ?");
            params.add(clean(locationId));
        }
        if (formatYearForDb(year) != null) {
            where.append(" AND FINANCIAL_YEAR = ?");
            params.add(formatYearForDb(year));
        }

        if (clean(revision) != null && !revision.equalsIgnoreCase("All") && !revision.equalsIgnoreCase("Latest")) {
            where.append(" AND REVISION_NO = ?");
            try {
                params.add(Integer.parseInt(revision));
            } catch (NumberFormatException e) {
                // Ignore invalid revision
            }
        }

        String countSql = "SELECT COUNT(*) FROM DMS_CAPEX_BUDGET" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        java.util.List<Object> queryParams = new java.util.ArrayList<>(params);
        
        String dataSql = "SELECT BUDGET_CODE, DOC_DATE, REVISION_NO, CREATED_BY, CREATED_ON, FILE_NAME, FILE_PATH FROM DMS_CAPEX_BUDGET " + where + " ORDER BY BUDGET_CODE ASC, REVISION_NO DESC";
        
        if (clean(revision) != null && revision.equalsIgnoreCase("Latest")) {
             dataSql += " OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY";
        } else {
             dataSql += " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
             queryParams.add(pageable.getOffset());
             queryParams.add(pageable.getPageSize());
        }

        List<CapexBudgetResponse> rows = jdbcTemplate.query(
            dataSql,
            (rs, rowNum) -> CapexBudgetResponse.builder()
                .budgetCode(rs.getString("BUDGET_CODE"))
                .docDate(rs.getTimestamp("DOC_DATE") != null ? rs.getTimestamp("DOC_DATE").toLocalDateTime() : null)
                .revisionNo(rs.getInt("REVISION_NO"))
                .fileName(rs.getString("FILE_NAME"))
                .createdBy(rs.getString("CREATED_BY"))
                .createdOn(rs.getTimestamp("CREATED_ON") != null ? rs.getTimestamp("CREATED_ON").toLocalDateTime() : null)
                .build(),
            queryParams.toArray());

        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }
    
    public File getFile(String budgetCode, String revision) {
        CapexBudgetResponse existing;
        if (revision != null) {
            existing = findLocalByCodeAndRevision(budgetCode, Integer.parseInt(revision));
        } else {
            existing = findLocalByCode(budgetCode);
        }
        if (existing != null && existing.getFilePath() != null) {
            File f = new File(existing.getFilePath());
            if (f.exists()) return f;
        }
        return null;
    }

    public java.util.List<java.util.Map<String, Object>> debugTable() {
        return jdbcTemplate.queryForList("SELECT * FROM DMS_CAPEX_BUDGET");
    }
}
