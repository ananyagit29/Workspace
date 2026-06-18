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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ipca.dms_api.dto.InvoiceDocumentResponse;

import jakarta.annotation.PostConstruct;

@Service
public class InvoiceDocumentService {

    private static final long MAX_FILE_SIZE = 1024 * 1024;

    private final JdbcTemplate invoiceJdbcTemplate;

    @Value("${dms.upload.directory}")
    private String uploadBaseDir;

    public InvoiceDocumentService(@Qualifier("primaryDataSource") DataSource invoiceDataSource) {
        this.invoiceJdbcTemplate = new JdbcTemplate(java.util.Objects.requireNonNull(invoiceDataSource));
    }

    // Expose JdbcTemplate for ad-hoc testing if needed
    public JdbcTemplate getJdbcTemplate() {
        return this.invoiceJdbcTemplate;
    }

    @PostConstruct
    public void init() {
        // Tables are managed externally in the Oracle Database.
    }

    // ─────────────────────────────────────────────────────────────
    // EXISTS: check if an invoice exists in SCM ERP (IPCASCMDRDB)
    // ─────────────────────────────────────────────────────────────
    public boolean exists(String invoiceNumber, String year, String locationId) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM scm_excise_invoice_header@IPCASCMDRDB WHERE UPPER(doc_code) = UPPER(?)");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(clean(invoiceNumber));

        // Map DMS locationId to SCM entity_code
        if (locationId != null && !locationId.isEmpty()) {
            sql.append(" AND entity_code = ?");
            params.add(locationId);
        }
        
        if (year != null && year.matches("\\d{4}-\\d{4}")) {
            String[] parts = year.split("-");
            int y1 = Integer.parseInt(parts[0]);
            int y2 = Integer.parseInt(parts[1]);
            sql.append(" AND doc_date BETWEEN ? AND ?");
            params.add(java.sql.Date.valueOf(y1 + "-04-01"));
            params.add(java.sql.Date.valueOf(y2 + "-03-31"));
        }

        try {
            Integer count = invoiceJdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("Error checking exists in IPCASCMDRDB: " + e.getMessage());
            return false;
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // SUGGEST: always query IPCASCMDRDB for invoice number autocomplete
    // ────────────────────────────────────────────────────────────────────
    public List<String> suggestInvoiceNumbers(String query, String locationId, String year) {
        String q = query != null ? query.trim() : "";

        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT s.doc_code FROM scm_excise_invoice_header@IPCASCMDRDB s " +
            "INNER JOIN DMS_INVOICE_DOCUMENTS d ON UPPER(d.INVOICE_NUMBER) = UPPER(s.doc_code) " +
            "WHERE UPPER(s.doc_code) LIKE ? AND d.FILE_PATH IS NOT NULL AND d.OTHER_FILE_PATH IS NOT NULL");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add("%" + q.toUpperCase() + "%");

        // Map DMS locationId to SCM entity_code
        if (locationId != null && !locationId.isEmpty()) {
            sql.append(" AND entity_code = ?");
            params.add(locationId);
        }

        if (year != null && year.matches("\\d{4}-\\d{4}")) {
            String[] parts = year.split("-");
            int y1 = Integer.parseInt(parts[0]);
            int y2 = Integer.parseInt(parts[1]);
            sql.append(" AND doc_date BETWEEN ? AND ?");
            params.add(java.sql.Date.valueOf(y1 + "-04-01"));
            params.add(java.sql.Date.valueOf(y2 + "-03-31"));
        }
        sql.append(" ORDER BY doc_code FETCH FIRST 15 ROWS ONLY");

        java.util.List<String> results = new java.util.ArrayList<>();
        try {
            System.out.println("[SUGGEST] SQL=" + sql + " params=" + params);
            results = invoiceJdbcTemplate.queryForList(sql.toString(), String.class, params.toArray());
            System.out.println("[SUGGEST] returned " + results.size() + " results");
        } catch (Exception e) {
            System.err.println("Error suggesting from IPCASCMDRDB: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    // ────────────────────────────────────────────────────────────────────────────
    // SEARCH: query IPCASCMDRDB for invoice list (with pagination)
    // Maps SCM columns to our InvoiceDocumentResponse DTO:
    //   doc_code        -> invoiceNumber
    //   entity_code     -> companyId
    //   division_code   -> divisionName
    //   doc_date        -> createdOn
    //   doc_amount      -> (unused, but available)
    //   store_code      -> locationId
    //   checked_by      -> createdBy
    //   We also LEFT JOIN to DMS_INVOICE_DOCUMENTS to get any locally attached files
    // ────────────────────────────────────────────────────────────────────────────
    public Page<InvoiceDocumentResponse> search(String invoiceNumber, String year, String locationId,
                                                 int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String invoiceFilter = emptyToNull(invoiceNumber);

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (invoiceFilter != null) {
            where.append(" AND UPPER(s.doc_code) LIKE UPPER(?) ");
            params.add("%" + invoiceFilter + "%");
        }
        
        // Map DMS locationId to SCM entity_code
        if (locationId != null && !locationId.isEmpty()) {
            where.append(" AND s.entity_code = ? ");
            params.add(locationId);
        }
        if (year != null && year.matches("\\d{4}-\\d{4}")) {
            String[] parts = year.split("-");
            int y1 = Integer.parseInt(parts[0]);
            int y2 = Integer.parseInt(parts[1]);
            where.append(" AND s.doc_date BETWEEN ? AND ? ");
            params.add(java.sql.Date.valueOf(y1 + "-04-01"));
            params.add(java.sql.Date.valueOf(y2 + "-03-31"));
        }

        // Count query
        String countSql = "SELECT COUNT(*) FROM scm_excise_invoice_header@IPCASCMDRDB s " +
                          "INNER JOIN DMS_INVOICE_DOCUMENTS d ON UPPER(d.INVOICE_NUMBER) = UPPER(s.doc_code) " +
                          where + " AND d.FILE_PATH IS NOT NULL AND d.OTHER_FILE_PATH IS NOT NULL";
        Long total = invoiceJdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // Data query with INNER JOIN to local DMS_INVOICE_DOCUMENTS for file info
        java.util.List<Object> queryParams = new java.util.ArrayList<>(params);
        queryParams.add(pageable.getOffset());
        queryParams.add(pageable.getPageSize());

        String dataSql = """
            SELECT s.doc_code, s.entity_code, s.division_code, s.store_code,
                   s.doc_date, s.checked_by, s.doc_amount,
                   d.FILE_NAME, d.FILE_PATH, d.OTHER_FILE_NAME, d.OTHER_FILE_PATH,
                   d.APPLICATION_NAME, d.CREATED_BY AS DMS_CREATED_BY, d.CREATED_ON AS DMS_CREATED_ON
            FROM scm_excise_invoice_header@IPCASCMDRDB s
            INNER JOIN DMS_INVOICE_DOCUMENTS d ON UPPER(d.INVOICE_NUMBER) = UPPER(s.doc_code)
            """ + where + " AND d.FILE_PATH IS NOT NULL AND d.OTHER_FILE_PATH IS NOT NULL ORDER BY s.doc_date DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        System.out.println("[SEARCH] SQL=" + dataSql);
        System.out.println("[SEARCH] params=" + queryParams);

        List<InvoiceDocumentResponse> rows = invoiceJdbcTemplate.query(dataSql,
            (rs, rowNum) -> InvoiceDocumentResponse.builder()
                .invoiceNumber(rs.getString("doc_code"))
                .companyId(rs.getString("entity_code"))
                .divisionName(rs.getString("division_code"))
                .locationId(rs.getString("store_code"))
                .createdBy(rs.getString("DMS_CREATED_BY") != null ? rs.getString("DMS_CREATED_BY") : rs.getString("checked_by"))
                .createdOn(rs.getTimestamp("DMS_CREATED_ON") != null
                    ? rs.getTimestamp("DMS_CREATED_ON").toLocalDateTime()
                    : (rs.getTimestamp("doc_date") != null ? rs.getTimestamp("doc_date").toLocalDateTime() : null))
                .invoiceFileName(rs.getString("FILE_NAME"))
                .fileName(rs.getString("OTHER_FILE_NAME"))
                .filePath(rs.getString("OTHER_FILE_PATH"))
                .otherFileName(rs.getString("OTHER_FILE_NAME"))
                .otherFilePath(rs.getString("OTHER_FILE_PATH"))
                .build(),
            queryParams.toArray());

        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    // ────────────────────────────────────────────────────────────────────
    // SAVE: validate invoice exists in SCM, then insert into local DMS
    // ────────────────────────────────────────────────────────────────────
    public InvoiceDocumentResponse saveInvoice(
            String invoiceNumber,
            String companyId,
            String locationId,
            String divisionName,
            String applicationName,
            String userId,
            MultipartFile invoiceFile) throws IOException {

        String cleanedInvoice = clean(invoiceNumber);
        if (cleanedInvoice == null || cleanedInvoice.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }
        if (invoiceFile == null || invoiceFile.isEmpty()) {
            throw new IllegalArgumentException("Invoice PDF is required.");
        }
        if (invoiceFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be 1 MB or less.");
        }
        String originalName = Objects.requireNonNull(invoiceFile.getOriginalFilename(), "File name is required.");
        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        // Check if invoice number exists in SCM ERP
        if (!exists(cleanedInvoice, null, locationId)) {
            throw new IllegalArgumentException(
                "Invoice number " + cleanedInvoice + " does not exist in the SCM ERP system for the selected location.");
        }

        // Check if already saved locally
        if (localRecordExists(cleanedInvoice)) {
            throw new IllegalArgumentException("Invoice number already exists in DMS.");
        }

        String safeFileName = cleanedInvoice + ".pdf";
        String targetDir = "d:/Workspace/Docs/Invoice";
        Path fullPath = Paths.get(targetDir, safeFileName);

        LocalDateTime now = LocalDateTime.now();
        invoiceJdbcTemplate.update("""
                INSERT INTO DMS_INVOICE_DOCUMENTS
                (INVOICE_NUMBER, FILE_NAME, FILE_PATH, COMPANY_ID, LOCATION_ID, DIVISION_NAME, APPLICATION_NAME, CREATED_BY, CREATED_ON)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                cleanedInvoice, safeFileName, fullPath.toString(), companyId, locationId,
                divisionName, applicationName, userId, Timestamp.valueOf(now));

        return findByInvoiceNumber(cleanedInvoice);
    }

    // ────────────────────────────────────────────────────────────────────
    // ATTACH OTHER FILE
    // ────────────────────────────────────────────────────────────────────
    public InvoiceDocumentResponse attachOtherFile(String invoiceNumber, String userId, MultipartFile otherFile) throws IOException {
        String cleanedInvoice = clean(invoiceNumber);
        if (cleanedInvoice == null || cleanedInvoice.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }

        // Check local DMS record exists
        InvoiceDocumentResponse parent = findByInvoiceNumberSafe(cleanedInvoice);
        if (parent == null) {
            throw new IllegalArgumentException(cleanedInvoice + ".pdf does not exist");
        }

        if (parent.getOtherFileName() != null && !parent.getOtherFileName().isEmpty()) {
            throw new IllegalArgumentException("Only one attached file is allowed per invoice. This invoice already has an attachment.");
        }

        if (otherFile == null || otherFile.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        if (otherFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be 1 MB or less.");
        }
        String originalName = Objects.requireNonNull(otherFile.getOriginalFilename(), "File name is required.");
        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        String safeFileName = originalName.replaceAll("[&,]", "-");
        String uploadDir = "d:/Workspace/Docs/Other";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Path fullPath = Paths.get(uploadDir, safeFileName);
        Files.write(fullPath, otherFile.getBytes());

        invoiceJdbcTemplate.update("""
                UPDATE DMS_INVOICE_DOCUMENTS 
                SET OTHER_FILE_NAME = ?, OTHER_FILE_PATH = ? 
                WHERE UPPER(INVOICE_NUMBER) = UPPER(?)
                """,
                safeFileName, fullPath.toString(), cleanedInvoice);

        return findByInvoiceNumber(cleanedInvoice);
    }

    // ────────────────────────────────────────────────────────────────────
    // DELETE OTHER FILE
    // ────────────────────────────────────────────────────────────────────
    public void deleteOtherFile(String invoiceNumber) {
        String cleanedInvoice = clean(invoiceNumber);
        if (cleanedInvoice == null || cleanedInvoice.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }

        InvoiceDocumentResponse parent = findByInvoiceNumberSafe(cleanedInvoice);
        if (parent == null || parent.getOtherFileName() == null || parent.getOtherFileName().isEmpty()) {
            throw new IllegalArgumentException("No other file attached to this invoice.");
        }

        String otherFilePath = parent.getOtherFilePath();

        if (otherFilePath != null && !otherFilePath.isEmpty()) {
            try {
                Files.deleteIfExists(Paths.get(otherFilePath));
            } catch (IOException e) {
                System.err.println("Warning: Could not delete file from disk: " + otherFilePath);
            }
        }

        invoiceJdbcTemplate.update("UPDATE DMS_INVOICE_DOCUMENTS SET OTHER_FILE_NAME = NULL, OTHER_FILE_PATH = NULL WHERE UPPER(INVOICE_NUMBER) = UPPER(?)", cleanedInvoice);
    }

    // ────────────────────────────────────────────────────────────────────
    // REPLACE OTHER FILE
    // ────────────────────────────────────────────────────────────────────
    public InvoiceDocumentResponse replaceOtherFile(String invoiceNumber, String userId, MultipartFile newFile) throws IOException {
        String cleanedInvoice = clean(invoiceNumber);
        if (cleanedInvoice == null || cleanedInvoice.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }

        InvoiceDocumentResponse parent = findByInvoiceNumberSafe(cleanedInvoice);
        if (parent == null) {
            throw new IllegalArgumentException("Invoice number does not exist in DMS.");
        }

        // Delete the existing file first if it exists physically
        String oldFilePath = parent.getOtherFilePath();
        if (oldFilePath != null && !oldFilePath.isEmpty()) {
            try {
                Files.deleteIfExists(Paths.get(oldFilePath));
            } catch (IOException e) {
                System.err.println("Warning: Could not delete old file from disk: " + oldFilePath);
            }
        }

        if (newFile == null || newFile.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        if (newFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be 1 MB or less.");
        }
        String originalName = Objects.requireNonNull(newFile.getOriginalFilename(), "File name is required.");
        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }
        String safeFileName = originalName.replaceAll("[&,]", "-");
        String uploadDir = "d:/Workspace/Docs/Other";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Path fullPath = Paths.get(uploadDir, safeFileName);
        Files.write(fullPath, newFile.getBytes());

        invoiceJdbcTemplate.update("""
                UPDATE DMS_INVOICE_DOCUMENTS 
                SET OTHER_FILE_NAME = ?, OTHER_FILE_PATH = ? 
                WHERE UPPER(INVOICE_NUMBER) = UPPER(?)
                """,
                safeFileName, fullPath.toString(), cleanedInvoice);

        return findByInvoiceNumber(cleanedInvoice);
    }

    // ────────────────────────────────────────────────────────────────────
    // FIND BY INVOICE NUMBER (from local DMS table)
    // ────────────────────────────────────────────────────────────────────
    public InvoiceDocumentResponse findByInvoiceNumber(String invoiceNumber) {
        return invoiceJdbcTemplate.queryForObject(
                "SELECT * FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = UPPER(?)",
                (rs, rowNum) -> mapRow(
                        rs.getString("INVOICE_NUMBER"),
                        rs.getString("FILE_NAME"),
                        rs.getString("FILE_PATH"),
                        rs.getString("COMPANY_ID"),
                        rs.getString("LOCATION_ID"),
                        rs.getString("DIVISION_NAME"),
                        rs.getString("APPLICATION_NAME"),
                        rs.getString("CREATED_BY"),
                        rs.getTimestamp("CREATED_ON"),
                        rs.getString("OTHER_FILE_NAME"),
                        rs.getString("OTHER_FILE_PATH")),
                invoiceNumber);
    }

    /**
     * Safe version that returns null instead of throwing when record doesn't exist
     */
    public InvoiceDocumentResponse findByInvoiceNumberSafe(String invoiceNumber) {
        try {
            return findByInvoiceNumber(invoiceNumber);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a record already exists in the local DMS_INVOICE_DOCUMENTS table
     */
    private boolean localRecordExists(String invoiceNumber) {
        try {
            Integer count = invoiceJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = UPPER(?)",
                Integer.class, invoiceNumber);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // FILE PATH LOOKUP (from local DMS table)
    // ────────────────────────────────────────────────────────────────────
    public String getFilePath(String invoiceNumber, String type) {
        if ("other".equalsIgnoreCase(type)) {
            try {
                String fileName = invoiceJdbcTemplate.queryForObject(
                    "SELECT OTHER_FILE_NAME FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = UPPER(?)",
                    String.class,
                    invoiceNumber);
                if (fileName == null || fileName.isEmpty()) return null;
                return "d:/Workspace/Docs/Other/" + fileName;
            } catch (Exception e) {
                return null;
            }
        } else {
            try {
                String fileName = invoiceJdbcTemplate.queryForObject(
                    "SELECT FILE_NAME FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = UPPER(?)",
                    String.class,
                    invoiceNumber);
                if (fileName == null || fileName.isEmpty()) return null;
                return "d:/Workspace/Docs/Invoice/" + fileName;
            } catch (Exception e) {
                return null;
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────────────────────────────
    private InvoiceDocumentResponse mapRow(
            String invoiceNumber,
            String fileName,
            String filePath,
            String companyId,
            String locationId,
            String divisionName,
            String applicationName,
            String createdBy,
            Timestamp createdOn,
            String otherFileName,
            String otherFilePath) {
        return InvoiceDocumentResponse.builder()
                .invoiceNumber(invoiceNumber)
                .fileName(fileName)
                .filePath(filePath)
                .companyId(companyId)
                .locationId(locationId)
                .divisionName(divisionName)
                .applicationName(applicationName)
                .createdBy(createdBy)
                .createdOn(createdOn == null ? null : createdOn.toLocalDateTime())
                .invoiceFileName(fileName)
                .otherFileName(otherFileName)
                .otherFilePath(otherFilePath)
                .build();
    }

    private String clean(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
