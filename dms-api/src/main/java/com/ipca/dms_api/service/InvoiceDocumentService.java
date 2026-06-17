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

    @PostConstruct
    public void init() {
        // Tables are managed externally in the Oracle Database.
    }

    public boolean exists(String invoiceNumber, String year) {
        String where = " WHERE UPPER(INVOICE_NUMBER) = UPPER(?) ";
        List<Object> params = new java.util.ArrayList<>();
        params.add(clean(invoiceNumber));

        if (year != null && year.matches("\\d{4}-\\d{4}")) {
            String[] parts = year.split("-");
            int y1 = Integer.parseInt(parts[0]);
            int y2 = Integer.parseInt(parts[1]);
            java.time.LocalDateTime startDate = java.time.LocalDateTime.of(y1, 4, 1, 0, 0, 0);
            java.time.LocalDateTime endDate = java.time.LocalDateTime.of(y2, 3, 31, 23, 59, 59);
            where += "AND CREATED_ON >= ? AND CREATED_ON <= ? ";
            params.add(java.sql.Timestamp.valueOf(startDate));
            params.add(java.sql.Timestamp.valueOf(endDate));
        }

        Integer count = invoiceJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DMS_INVOICE_DOCUMENTS" + where,
                Integer.class,
                params.toArray());
        return count != null && count > 0;
    }

    public List<String> suggestInvoiceNumbers(String query, boolean strict) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        String q = query.trim().toUpperCase();
        File folder = new File("d:/Workspace/Docs/Invoice");
        if (!folder.exists() || !folder.isDirectory()) {
            return List.of();
        }
        
        java.util.List<String> results = new java.util.ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String invNum = name.substring(0, name.length() - 4).toUpperCase();
                if (invNum.contains(q)) {
                    if (strict) {
                        Integer count = invoiceJdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = ? AND OTHER_FILE_NAME IS NOT NULL",
                                Integer.class, invNum);
                        if (count != null && count > 0) results.add(invNum);
                    } else {
                        results.add(invNum);
                    }
                    if (results.size() >= 15) break;
                }
            }
        }
        return results;
    }

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
        if (exists(cleanedInvoice, null)) {
            throw new IllegalArgumentException("Invoice number already exists.");
        }

        String safeFileName = cleanedInvoice + ".pdf";
        String targetDir = "d:/Workspace/Docs/Invoice";
        Path fullPath = Paths.get(targetDir, safeFileName);
        
        // We do NOT write the invoiceFile to disk because it is pre-defined in the Docs/Invoice folder.
        // Files.write(fullPath, invoiceFile.getBytes());

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

    public InvoiceDocumentResponse attachOtherFile(String invoiceNumber, String userId, MultipartFile otherFile) throws IOException {
        String cleanedInvoice = clean(invoiceNumber);
        if (cleanedInvoice == null || cleanedInvoice.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }
        
        InvoiceDocumentResponse parent = findByInvoiceNumber(cleanedInvoice);
        if (parent == null) {
            throw new IllegalArgumentException("Invoice number does not exist.");
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

    public void deleteOtherFile(String invoiceNumber) {
        String cleanedInvoice = clean(invoiceNumber);
        if (cleanedInvoice == null || cleanedInvoice.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }

        InvoiceDocumentResponse parent = findByInvoiceNumber(cleanedInvoice);
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

    public InvoiceDocumentResponse replaceOtherFile(String invoiceNumber, String userId, MultipartFile newFile) throws IOException {
        String cleanedInvoice = clean(invoiceNumber);
        if (cleanedInvoice == null || cleanedInvoice.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }
        
        InvoiceDocumentResponse parent = findByInvoiceNumber(cleanedInvoice);
        if (parent == null) {
            throw new IllegalArgumentException("Invoice number does not exist.");
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

    public Page<InvoiceDocumentResponse> search(String invoiceNumber, String locationId, String year, int page, int size, boolean strict) {
        Pageable pageable = PageRequest.of(page, size);
        String invoiceFilter = emptyToNull(invoiceNumber);
        String locationFilter = emptyToNull(locationId);

        String where = " WHERE (? IS NULL OR UPPER(INVOICE_NUMBER) LIKE UPPER(?)) "
                + "AND (? IS NULL OR LOCATION_ID = ?) ";
        List<Object> params = new java.util.ArrayList<>();
        params.add(invoiceFilter);
        params.add(invoiceFilter == null ? null : "%" + invoiceFilter + "%");
        params.add(locationFilter);
        params.add(locationFilter);

        if (year != null && year.matches("\\d{4}-\\d{4}")) {
            String[] parts = year.split("-");
            int y1 = Integer.parseInt(parts[0]);
            int y2 = Integer.parseInt(parts[1]);
            java.time.LocalDateTime startDate = java.time.LocalDateTime.of(y1, 4, 1, 0, 0, 0);
            java.time.LocalDateTime endDate = java.time.LocalDateTime.of(y2, 3, 31, 23, 59, 59);
            where += "AND CREATED_ON >= ? AND CREATED_ON <= ? ";
            params.add(java.sql.Timestamp.valueOf(startDate));
            params.add(java.sql.Timestamp.valueOf(endDate));
        }

        if (strict) {
            where += "AND FILE_NAME IS NOT NULL AND OTHER_FILE_NAME IS NOT NULL ";
        }

        Long total = invoiceJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DMS_INVOICE_DOCUMENTS " + where,
                Long.class,
                params.toArray());

        List<Object> queryParams = new java.util.ArrayList<>(params);
        queryParams.add(pageable.getOffset());
        queryParams.add(pageable.getPageSize());

        List<InvoiceDocumentResponse> rows = invoiceJdbcTemplate.query("""
                SELECT INVOICE_NUMBER, FILE_NAME, FILE_PATH, 
                       COMPANY_ID, LOCATION_ID, DIVISION_NAME, APPLICATION_NAME, 
                       CREATED_BY, CREATED_ON, OTHER_FILE_NAME, OTHER_FILE_PATH
                FROM DMS_INVOICE_DOCUMENTS 
                """ + where + " ORDER BY CREATED_ON DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (rs, rowNum) -> InvoiceDocumentResponse.builder()
                        .invoiceNumber(rs.getString("INVOICE_NUMBER"))
                        .fileName(rs.getString("OTHER_FILE_NAME")) // for backward compatibility
                        .filePath(rs.getString("OTHER_FILE_PATH"))
                        .companyId(rs.getString("COMPANY_ID"))
                        .locationId(rs.getString("LOCATION_ID"))
                        .divisionName(rs.getString("DIVISION_NAME"))
                        .applicationName(rs.getString("APPLICATION_NAME"))
                        .createdBy(rs.getString("CREATED_BY"))
                        .createdOn(rs.getTimestamp("CREATED_ON") == null ? null : rs.getTimestamp("CREATED_ON").toLocalDateTime())
                        .invoiceFileName(rs.getString("FILE_NAME"))
                        .otherFileName(rs.getString("OTHER_FILE_NAME"))
                        .otherFilePath(rs.getString("OTHER_FILE_PATH"))
                        .build(),
                queryParams.toArray());

        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

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
    
    public String getFilePath(String invoiceNumber, String type) {
        if ("other".equalsIgnoreCase(type)) {
            String fileName = invoiceJdbcTemplate.queryForObject(
                    "SELECT OTHER_FILE_NAME FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = UPPER(?)",
                    String.class,
                    invoiceNumber);
            if (fileName == null || fileName.isEmpty()) return null;
            return "d:/Workspace/Docs/Other/" + fileName;
        } else {
            String fileName = invoiceJdbcTemplate.queryForObject(
                    "SELECT FILE_NAME FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = UPPER(?)",
                    String.class,
                    invoiceNumber);
            if (fileName == null || fileName.isEmpty()) return null;
            return "d:/Workspace/Docs/Invoice/" + fileName;
        }
    }


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

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
