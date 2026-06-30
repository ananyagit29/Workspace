package com.ipca.dms_api.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountsService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${dms.upload.directory}")
    private String uploadBaseDir;

    public AccountsService(@Qualifier("primaryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String clean(String val) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : null;
    }

    /**
     * Convert financial year "2025-2026" + month "04"-"03" into doc_year strings
     * e.g. year="2025-2026", month="04" → "202504"
     *      year="2025-2026", month="01" → "202601"
     */
    private String[] computeDateRange(String year, String month) {
        if (year == null || !year.matches("\\d{4}-\\d{4}")) return null;
        String[] parts = year.split("-");
        String startYear = parts[0];
        String endYear = parts[1];
        if (month != null && !month.isEmpty()) {
            String yr = (month.equals("01") || month.equals("02") || month.equals("03")) ? endYear : startYear;
            String ym = yr + month;
            return new String[]{ym, ym};
        }
        return new String[]{startYear + "04", endYear + "03"};
    }

    private String[] computeDateRangeFromTo(String year, String fromMonth, String toMonth) {
        if (year == null || !year.matches("\\d{4}-\\d{4}")) return null;
        String[] parts = year.split("-");
        String startYear = parts[0];
        String endYear = parts[1];
        String from = (fromMonth.equals("01") || fromMonth.equals("02") || fromMonth.equals("03"))
                ? endYear + fromMonth : startYear + fromMonth;
        String to = (toMonth.equals("01") || toMonth.equals("02") || toMonth.equals("03"))
                ? endYear + toMonth : startYear + toMonth;
        return new String[]{from, to};
    }

    // ── 1. Daybooks ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> getDaybooks() {
        String sql = "SELECT i.name, i.code FROM ipcaprod.fa_daybooks@ipcascmdb i, DMS_GENERAL_PARAMETERS d " +
                     "WHERE i.code = d.PARAMETER_NAME AND d.APPLICATION_NAME='ACCOUNTS' " +
                     "AND d.PARAMETER_NAME <> 'Year' ORDER BY i.CODE";
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            System.err.println("[Accounts] getDaybooks error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 2. Doc list for Create (un-uploaded from SCM) ───────────────────────

    public List<Map<String, Object>> getDocList(String locationId, String daybookCode, String year, String month) {
        String[] range = computeDateRange(year, month);
        if (range == null) return new ArrayList<>();

        String sql = "SELECT th.doc_code, th.doc_date, th.doc_year, u.user_name AS fas_user " +
                     "FROM IPCAPROD.FA_TRANSACTION_HEADER@IPCASCMDB th, commondata.scm_users@IPCASCMDB u " +
                     "WHERE th.entity_code = ? AND th.daybook_code = ? " +
                     "AND th.doc_year BETWEEN ? AND ? " +
                     "AND NOT EXISTS (SELECT 'x' FROM DMS_ACCOUNTS " +
                     "WHERE location_id = th.entity_code AND doc_year = th.doc_year " +
                     "AND doc_code = th.doc_code AND daybook_code = th.daybook_code) " +
                     "AND th.user_id = u.user_id (+) " +
                     "AND (CASE WHEN th.RECEIPT_PAYMENT IS NULL THEN 'R' ELSE th.receipt_payment END) <> 'O' " +
                     "GROUP BY th.doc_code, th.doc_date, th.doc_year, u.user_name " +
                     "ORDER BY to_number(th.doc_code)";
        try {
            return jdbcTemplate.queryForList(sql, locationId, daybookCode, range[0], range[1]);
        } catch (Exception e) {
            System.err.println("[Accounts] getDocList error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 3. Doc details for Create (account name, bill number, etc.) ─────────

    public Map<String, Object> getDocDetails(String companyId, String locationId, String daybookCode,
                                              String docCode, String year, String month) {
        String[] range = computeDateRange(year, month);
        if (range == null) return null;

        // First get doc_date and doc_year from header
        String headerSql = "SELECT th.doc_code, th.doc_date, th.doc_year " +
                           "FROM IPCAPROD.FA_TRANSACTION_HEADER@IPCASCMDB th " +
                           "WHERE th.entity_code = ? AND th.daybook_code = ? AND th.doc_code = ? " +
                           "AND th.doc_year BETWEEN ? AND ? " +
                           "AND NOT EXISTS (SELECT 'x' FROM DMS_ACCOUNTS " +
                           "WHERE location_id = th.entity_code AND doc_year = th.doc_year " +
                           "AND doc_code = th.doc_code AND daybook_code = th.daybook_code) " +
                           "AND (CASE WHEN th.RECEIPT_PAYMENT IS NULL THEN 'R' ELSE th.receipt_payment END) <> 'O' " +
                           "AND ROWNUM = 1";
        try {
            List<Map<String, Object>> headerRows = jdbcTemplate.queryForList(headerSql, locationId, daybookCode, docCode, range[0], range[1]);
            if (headerRows.isEmpty()) return null;

            Map<String, Object> header = headerRows.get(0);

            // Detail query — get account_name, bill_number, etc.
            String detailSql = "SELECT td.account_code, " +
                    "MAX((SELECT MAX(ah.name) FROM IPCAPROD.account_head@IPCASCMDB ah, " +
                    "IPCAPROD.entity@IPCASCMDB e " +
                    "WHERE ah.code = td.account_code AND (ah.control_code = td.control_code OR ah.control_code = (CASE td.control_code WHEN '0' THEN '0' ELSE '3' END)) " +
                    "AND ah.ENTITY_CODE = (CASE WHEN td.control_code = 1 THEN (CASE WHEN NVL(e.TYPES,'X') = 'D' THEN td.ENTITY_CODE ELSE '*' END) ELSE '*' END))) " +
                    "AS account_name, " +
                    "td.doc_code, td.doc_date, td.doc_year, td.bill_number, td.bill_date, " +
                    "SUM(td.tran_amount) AS tran_amount " +
                    "FROM IPCAPROD.fa_transaction_detail@IPCASCMDB td " +
                    "WHERE td.company_code = ? AND td.entity_code = ? " +
                    "AND td.daybook_code = ? AND td.doc_code = ? " +
                    "AND td.doc_year BETWEEN ? AND ? " +
                    "AND td.tran_serial = (CASE daybook_code " +
                    "WHEN '12' THEN 1 WHEN '13' THEN 0 " +
                    "WHEN '21' THEN 0 WHEN '22' THEN 0 " +
                    "WHEN '26' THEN 0 WHEN '27' THEN 0 " +
                    "WHEN '31' THEN 0 WHEN '35' THEN 1 " +
                    "WHEN '41' THEN 0 WHEN '56' THEN 0 " +
                    "WHEN '57' THEN 1 WHEN '57B' THEN 1 " +
                    "WHEN '12A' THEN 0 WHEN '22A' THEN 0 " +
                    "WHEN '22B' THEN 0 " +
                    "ELSE 2 END) " +
                    "AND dr_cr = (CASE daybook_code WHEN '13' THEN 1 WHEN '12' THEN 1 WHEN '12A' THEN 1 ELSE -1 END) " +
                    "GROUP BY td.doc_code, td.doc_date, td.daybook_code, td.account_code, " +
                    "td.doc_year, td.bill_number, td.bill_date, td.doc_year " +
                    "ORDER BY to_number(doc_code), doc_date";
            List<Map<String, Object>> detailRows = jdbcTemplate.queryForList(detailSql, companyId, locationId, daybookCode, docCode, range[0], range[1]);

            java.util.HashMap<String, Object> result = new java.util.HashMap<>(header);
            if (!detailRows.isEmpty()) {
                Map<String, Object> detail = detailRows.get(0);
                result.put("ACCOUNT_NAME", detail.get("ACCOUNT_NAME"));
                result.put("ACCOUNT_CODE", detail.get("ACCOUNT_CODE"));
                result.put("BILL_NUMBER", detail.get("BILL_NUMBER"));
                result.put("BILL_DATE", detail.get("BILL_DATE"));
                result.put("TRAN_AMOUNT", detail.get("TRAN_AMOUNT"));
            }
            return result;
        } catch (Exception e) {
            System.err.println("[Accounts] getDocDetails error: " + e.getMessage());
            return null;
        }
    }

    // ── 4. Check if daybook requires Account Name / Bill Number fields ──────

    public boolean isFieldRequired(String daybookCode) {
        try {
            String sql = "SELECT Required FROM DMS_GENERAL_PARAMETERS WHERE APPLICATION_NAME='ACCOUNTS' AND PARAMETER_NAME=?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, daybookCode);
            if (!rows.isEmpty()) {
                Object val = rows.get(0).get("REQUIRED");
                return val != null && val.toString().equalsIgnoreCase("YES");
            }
        } catch (Exception e) {
            System.err.println("[Accounts] isFieldRequired error: " + e.getMessage());
        }
        return false;
    }

    // ── 5. Upload document ──────────────────────────────────────────────────

    public String uploadDocument(String companyId, String locationId, String divisionName,
                                  String applicationName, String financialYear,
                                  String daybookCode, String daybookName,
                                  String accountCode, String accountName,
                                  String docCode, String docDate, String docYear,
                                  String billNumber, String billDate, double tranAmount,
                                  String docMonth, String createdBy, MultipartFile file) throws IOException {

        // Build upload path: baseDir/app/com/loc/finyear/daybookCode/docCode/
        String dirPath = Paths.get(uploadBaseDir, applicationName, companyId, locationId,
                                    financialYear, daybookCode, docCode).toString();
        File directory = new File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "document.pdf";

        // Construct filename: base-month.pdf
        String baseName = originalFilename.contains(".")
                ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                : originalFilename;
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : ".pdf";
        String savedName = baseName.replaceAll("&", "and").replaceAll("'", "-") + "-" + docMonth + extension;
        String filePath = Paths.get(dirPath, savedName).toString();

        file.transferTo(new File(filePath));

        // Parse dates
        java.sql.Date sqlDocDate = null;
        java.sql.Date sqlBillDate = null;
        try {
            if (docDate != null && !docDate.equals("null") && !docDate.isEmpty()) {
                sqlDocDate = java.sql.Date.valueOf(docDate);
            }
        } catch (Exception ignored) {}
        try {
            if (billDate != null && !billDate.equals("null") && !billDate.isEmpty()) {
                sqlBillDate = java.sql.Date.valueOf(billDate);
            }
        } catch (Exception ignored) {}

        String insertSql = "INSERT INTO DMS_ACCOUNTS " +
                "(COMPANY_ID, LOCATION_ID, DIVISION_NAME, APPLICATION_NAME, FINANCIAL_YEAR, " +
                "DAYBOOK_CODE, DAYBOOK_NAME, ACCOUNT_CODE, ACCOUNT_NAME, DOC_CODE, DOC_DATE, " +
                "DOC_YEAR, BILL_NUMBER, BILL_DATE, TRAN_AMOUNT, CREATED_BY, CREATED_ON, FILE_NAME, FILE_PATH) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        jdbcTemplate.update(insertSql,
                companyId, locationId, divisionName, applicationName, financialYear,
                daybookCode, daybookName, accountCode, accountName, docCode,
                sqlDocDate, docYear, billNumber, sqlBillDate, tranAmount,
                createdBy, new Timestamp(System.currentTimeMillis()), savedName, filePath);

        return "File Uploaded Successfully";
    }

    // ── 6. Search uploaded documents ────────────────────────────────────────

    public List<Map<String, Object>> search(String locationId, String daybookCode,
                                             String year, String month, String docCode) {
        String[] range = computeDateRange(year, month);
        if (range == null) return new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT Daybook_Code, Daybook_Name, Account_Code, Account_Name, " +
                "Doc_Code, Doc_Date, Doc_Year, Bill_Number, Bill_Date, Tran_Amount, " +
                "Created_By, TO_CHAR(Created_On,'YYYY-MM-DD HH24:MI:SS') AS Created_On, " +
                "File_Name, File_Path " +
                "FROM DMS_ACCOUNTS " +
                "WHERE Location_Id = ? AND Daybook_Code = ? " +
                "AND Doc_Year BETWEEN ? AND ? ");
        List<Object> params = new ArrayList<>();
        params.add(locationId);
        params.add(daybookCode);
        params.add(range[0]);
        params.add(range[1]);

        if (clean(docCode) != null) {
            sql.append("AND Doc_Code = ? ");
            params.add(clean(docCode));
        }

        sql.append("AND DOC_CODE != 'Select' ORDER BY TO_NUMBER(Doc_Code)");

        try {
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            System.err.println("[Accounts] search error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 7. Remove document ──────────────────────────────────────────────────

    public void removeDocument(String daybookCode, String docMonth, String docYear,
                                String docCode, String filename) {
        try {
            // Get file path to delete the physical file
            String sql = "SELECT FILE_PATH FROM DMS_ACCOUNTS WHERE DAYBOOK_CODE = ? AND DOC_CODE = ? AND FILE_NAME = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, daybookCode, docCode, filename);
            if (!rows.isEmpty()) {
                Object pathObj = rows.get(0).get("FILE_PATH");
                if (pathObj != null) {
                    File f = new File(pathObj.toString());
                    if (f.exists()) {
                        Files.deleteIfExists(f.toPath());
                    }
                }
            }
            String deleteSql = "DELETE FROM DMS_ACCOUNTS WHERE DAYBOOK_CODE = ? AND DOC_CODE = ? AND FILE_NAME = ?";
            jdbcTemplate.update(deleteSql, daybookCode, docCode, filename);
        } catch (Exception e) {
            System.err.println("[Accounts] removeDocument error: " + e.getMessage());
        }
    }

    // ── 8. Party names for Report ───────────────────────────────────────────

    public List<Map<String, Object>> getPartyNames(String daybookCode, String financialYear, String locationId) {
        String sql = "SELECT ACCOUNT_CODE, ACCOUNT_NAME FROM DMS_ACCOUNTS " +
                     "WHERE DAYBOOK_CODE = ? AND FINANCIAL_YEAR = ? AND LOCATION_ID = ? " +
                     "AND ACCOUNT_CODE IS NOT NULL AND ACCOUNT_NAME IS NOT NULL " +
                     "GROUP BY ACCOUNT_CODE, ACCOUNT_NAME ORDER BY 2";
        try {
            return jdbcTemplate.queryForList(sql, daybookCode, financialYear, locationId);
        } catch (Exception e) {
            System.err.println("[Accounts] getPartyNames error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 9. Report: uploaded docs filtered (party wise / account code wise) ──

    public List<Map<String, Object>> getReportDocuments(String locationId, String daybookCode,
                                                         String year, String fromMonth, String toMonth,
                                                         String accName, String accCode, String amountMoreThan) {
        String[] range = computeDateRangeFromTo(year, fromMonth, toMonth);
        if (range == null) return new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT Daybook_Code, Daybook_Name, Account_Code, Account_Name, " +
                "Doc_Code, Doc_Date, Doc_Year, Bill_Number, Bill_Date, Tran_Amount, " +
                "Created_By, TO_CHAR(Created_On,'YYYY-MM-DD HH24:MI:SS') AS Created_On, " +
                "File_Name, File_Path " +
                "FROM DMS_ACCOUNTS " +
                "WHERE Location_Id = ? AND Daybook_Code = ? " +
                "AND Doc_Year BETWEEN ? AND ? ");
        List<Object> params = new ArrayList<>();
        params.add(locationId);
        params.add(daybookCode);
        params.add(range[0]);
        params.add(range[1]);

        if (clean(accName) != null) {
            sql.append("AND ACCOUNT_CODE = ? ");
            params.add(clean(accName));
        }
        if (clean(accCode) != null) {
            sql.append("AND ACCOUNT_CODE = ? ");
            params.add(clean(accCode));
        }
        if (clean(amountMoreThan) != null) {
            sql.append("AND TRAN_AMOUNT >= ? ");
            params.add(Double.parseDouble(amountMoreThan));
        }

        sql.append("AND DOC_CODE != 'Select' ORDER BY TO_NUMBER(Doc_Code)");

        try {
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            System.err.println("[Accounts] getReportDocuments error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 10. Missing documents for Report ────────────────────────────────────

    public List<Map<String, Object>> getMissingDocuments(String locationId, String daybookCode, String year) {
        String[] range = computeDateRange(year, null);
        if (range == null) return new ArrayList<>();

        String sql = "SELECT th.doc_code, th.doc_date, th.doc_year, u.user_name AS fas_user " +
                     "FROM IPCAPROD.FA_TRANSACTION_HEADER@IPCASCMDB th, commondata.scm_users@IPCASCMDB u " +
                     "WHERE th.entity_code = ? AND th.daybook_code = ? " +
                     "AND th.doc_year BETWEEN ? AND ? " +
                     "AND NOT EXISTS (SELECT 'x' FROM DMS_ACCOUNTS " +
                     "WHERE location_id = th.entity_code AND doc_year = th.doc_year " +
                     "AND doc_code = th.doc_code AND daybook_code = th.daybook_code) " +
                     "AND th.user_id = u.user_id (+) " +
                     "AND (CASE WHEN th.RECEIPT_PAYMENT IS NULL THEN 'R' ELSE th.receipt_payment END) <> 'O' " +
                     "GROUP BY th.doc_code, th.doc_date, th.doc_year, u.user_name " +
                     "ORDER BY to_number(th.doc_code)";
        try {
            return jdbcTemplate.queryForList(sql, locationId, daybookCode, range[0], range[1]);
        } catch (Exception e) {
            System.err.println("[Accounts] getMissingDocuments error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── File retrieval for viewing ──────────────────────────────────────────

    public File getFile(String daybookCode, String docCode, String fileName) {
        try {
            String sql = "SELECT FILE_PATH FROM DMS_ACCOUNTS WHERE DAYBOOK_CODE = ? AND DOC_CODE = ? AND FILE_NAME = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, daybookCode, docCode, fileName);
            if (!rows.isEmpty()) {
                Object pathObj = rows.get(0).get("FILE_PATH");
                if (pathObj != null) {
                    File f = new File(pathObj.toString());
                    if (f.exists()) return f;
                }
            }
        } catch (Exception e) {
            System.err.println("[Accounts] getFile error: " + e.getMessage());
        }
        return null;
    }
}
