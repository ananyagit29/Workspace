package com.ipca.dms_api.service;

import com.ipca.dms_api.dto.InvoiceDocumentResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

@Service
public class InvoiceDocumentService {

    private static final long MAX_FILE_SIZE = 1024 * 1024;

    private final JdbcTemplate invoiceJdbcTemplate;

    @Value("${dms.upload.directory}")
    private String uploadBaseDir;

    public InvoiceDocumentService(@Qualifier("invoiceDataSource") DataSource invoiceDataSource) {
        this.invoiceJdbcTemplate = new JdbcTemplate(invoiceDataSource);
    }

    @PostConstruct
    public void init() {
        invoiceJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS DMS_INVOICE_DOCUMENTS (
                    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                    INVOICE_NUMBER VARCHAR(50) NOT NULL UNIQUE,
                    FILE_NAME VARCHAR(150) NOT NULL,
                    FILE_PATH VARCHAR(500) NOT NULL,
                    COMPANY_ID VARCHAR(20),
                    LOCATION_ID VARCHAR(20),
                    DIVISION_NAME VARCHAR(60),
                    APPLICATION_NAME VARCHAR(80),
                    CREATED_BY VARCHAR(50),
                    CREATED_ON TIMESTAMP
                )
                """);
    }

    public boolean exists(String invoiceNumber) {
        Integer count = invoiceJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = UPPER(?)",
                Integer.class,
                clean(invoiceNumber));
        return count != null && count > 0;
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
        if (exists(cleanedInvoice)) {
            throw new IllegalArgumentException("Invoice number already exists.");
        }

        String safeFileName = originalName.replaceAll("[&,]", "-");
        String uploadDir = uploadBaseDir + "/InvoiceDocument/" + valueOrDefault(companyId, "NA") + "/"
                + valueOrDefault(locationId, "NA") + "/" + cleanedInvoice + "/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Path fullPath = Paths.get(uploadDir, safeFileName);
        Files.write(fullPath, invoiceFile.getBytes());

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

    public Page<InvoiceDocumentResponse> search(String invoiceNumber, String locationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String invoiceFilter = emptyToNull(invoiceNumber);
        String locationFilter = emptyToNull(locationId);

        String where = " WHERE (? IS NULL OR UPPER(INVOICE_NUMBER) LIKE UPPER(?)) "
                + "AND (? IS NULL OR LOCATION_ID = ?) ";

        Long total = invoiceJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DMS_INVOICE_DOCUMENTS" + where,
                Long.class,
                invoiceFilter, invoiceFilter == null ? null : "%" + invoiceFilter + "%",
                locationFilter, locationFilter);

        List<InvoiceDocumentResponse> rows = invoiceJdbcTemplate.query(
                "SELECT * FROM DMS_INVOICE_DOCUMENTS" + where
                        + "ORDER BY CREATED_ON DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapRow(
                        rs.getLong("ID"),
                        rs.getString("INVOICE_NUMBER"),
                        rs.getString("FILE_NAME"),
                        rs.getString("FILE_PATH"),
                        rs.getString("COMPANY_ID"),
                        rs.getString("LOCATION_ID"),
                        rs.getString("DIVISION_NAME"),
                        rs.getString("APPLICATION_NAME"),
                        rs.getString("CREATED_BY"),
                        rs.getTimestamp("CREATED_ON")),
                invoiceFilter, invoiceFilter == null ? null : "%" + invoiceFilter + "%",
                locationFilter, locationFilter, size, page * size);

        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    public InvoiceDocumentResponse findById(Long id) {
        return invoiceJdbcTemplate.queryForObject(
                "SELECT * FROM DMS_INVOICE_DOCUMENTS WHERE ID = ?",
                (rs, rowNum) -> mapRow(
                        rs.getLong("ID"),
                        rs.getString("INVOICE_NUMBER"),
                        rs.getString("FILE_NAME"),
                        rs.getString("FILE_PATH"),
                        rs.getString("COMPANY_ID"),
                        rs.getString("LOCATION_ID"),
                        rs.getString("DIVISION_NAME"),
                        rs.getString("APPLICATION_NAME"),
                        rs.getString("CREATED_BY"),
                        rs.getTimestamp("CREATED_ON")),
                id);
    }

    private InvoiceDocumentResponse findByInvoiceNumber(String invoiceNumber) {
        return invoiceJdbcTemplate.queryForObject(
                "SELECT * FROM DMS_INVOICE_DOCUMENTS WHERE UPPER(INVOICE_NUMBER) = UPPER(?)",
                (rs, rowNum) -> mapRow(
                        rs.getLong("ID"),
                        rs.getString("INVOICE_NUMBER"),
                        rs.getString("FILE_NAME"),
                        rs.getString("FILE_PATH"),
                        rs.getString("COMPANY_ID"),
                        rs.getString("LOCATION_ID"),
                        rs.getString("DIVISION_NAME"),
                        rs.getString("APPLICATION_NAME"),
                        rs.getString("CREATED_BY"),
                        rs.getTimestamp("CREATED_ON")),
                invoiceNumber);
    }

    private InvoiceDocumentResponse mapRow(
            Long id,
            String invoiceNumber,
            String fileName,
            String filePath,
            String companyId,
            String locationId,
            String divisionName,
            String applicationName,
            String createdBy,
            Timestamp createdOn) {
        return InvoiceDocumentResponse.builder()
                .id(id)
                .invoiceNumber(invoiceNumber)
                .fileName(fileName)
                .filePath(filePath)
                .companyId(companyId)
                .locationId(locationId)
                .divisionName(divisionName)
                .applicationName(applicationName)
                .createdBy(createdBy)
                .createdOn(createdOn == null ? null : createdOn.toLocalDateTime())
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
