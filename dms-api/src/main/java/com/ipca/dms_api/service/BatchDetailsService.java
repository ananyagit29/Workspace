package com.ipca.dms_api.service;

import com.ipca.dms_api.dto.*;
import com.ipca.dms_api.entity.BatchDetails;
import com.ipca.dms_api.repository.BatchDetailsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BatchDetailsService {

    private final BatchDetailsRepository batchRepo;
    private final JdbcTemplate jdbcTemplate;

    @Value("${dms.upload.directory}")
    private String uploadBaseDir;

    // ── GET PRODUCT DETAILS (from SCM DB via JdbcTemplate) ───────────────────
    public ProductDetailsResponse getProductDetails(String batchType, String productCode) {
        String query;

        if ("Own".equalsIgnoreCase(batchType) || "Loan License".equalsIgnoreCase(batchType)) {
            String typeFilter = "Own".equalsIgnoreCase(batchType) ? "F" : "L";
            query = "SELECT product_code, pr.name AS product_name, " +
                    "entity_code AS vendor_code, ent.name AS vendor_name " +
                    "FROM ipcaprod.scm_production_plan@ipcascmdb scm, " +
                    "ipcaprod.product@ipcascmdb pr, " +
                    "ipcaprod.entity@ipcascmdb ent " +
                    "WHERE pr.local_export = 'L' AND pr.obsolete = 'N' " +
                    "AND pr.is_sample = 'N' AND pr.division_code = 'F' " +
                    "AND scm.plan_month = (SELECT MAX(plan_month) " +
                    "   FROM ipcaprod.scm_production_plan@ipcascmdb x " +
                    "   WHERE x.entity_code = scm.entity_code " +
                    "   AND x.division_code = scm.division_code " +
                    "   AND x.product_code = scm.product_code) " +
                    "AND scm.product_code = pr.code " +
                    "AND scm.entity_code = ent.code " +
                    "AND scm.product_code = ? " +
                    "AND TYPES = ? " +
                    "GROUP BY entity_code, ent.name, product_code, pr.name " +
                    "ORDER BY product_code, pr.name";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, productCode, typeFilter);
            if (rows.isEmpty())
                return null;
            Map<String, Object> row = rows.get(0);
            return ProductDetailsResponse.builder()
                    .productCode((String) row.get("PRODUCT_CODE"))
                    .productName((String) row.get("PRODUCT_NAME"))
                    .vendorCode((String) row.get("VENDOR_CODE"))
                    .vendorName((String) row.get("VENDOR_NAME"))
                    .build();

        } else if ("Third Party".equalsIgnoreCase(batchType)) {
            query = "SELECT material_code AS product_code, pr.name AS product_name, " +
                    "vendor_code, ah.name AS vendor_name " +
                    "FROM ipcaprod.scm_purchase_detail@ipcascmdb scm, " +
                    "ipcaprod.account_head@ipcascmdb ah, " +
                    "ipcaprod.product@ipcascmdb pr " +
                    "WHERE scm.material_type = 'FG' " +
                    "AND ah.entity_code = '*' " +
                    "AND scm.vendor_code = ah.code " +
                    "AND scm.material_code = pr.code " +
                    "AND scm.doc_date = (SELECT MAX(doc_date) " +
                    "   FROM ipcaprod.scm_purchase_detail@ipcascmdb x " +
                    "   WHERE x.entity_code = scm.entity_code " +
                    "   AND x.division_code = scm.division_code " +
                    "   AND x.material_code = scm.material_code " +
                    "   AND NVL(close_order,'N') = 'N') " +
                    "AND scm.material_code = ? " +
                    "GROUP BY vendor_code, ah.name, material_code, pr.name " +
                    "ORDER BY material_code, pr.name";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, productCode);
            if (rows.isEmpty())
                return null;
            Map<String, Object> row = rows.get(0);
            return ProductDetailsResponse.builder()
                    .productCode((String) row.get("PRODUCT_CODE"))
                    .productName((String) row.get("PRODUCT_NAME"))
                    .vendorCode((String) row.get("VENDOR_CODE"))
                    .vendorName((String) row.get("VENDOR_NAME"))
                    .build();
        }

        return null;
    }

    // ── GET FILE NAMES for a batch ────────────────────────────────────────────
    public List<BatchFileResponse> getFileNames(String productCode, String batchType, String batchNumber) {
        List<BatchDetails> records = batchRepo.findFilesByBatch(productCode, batchType, batchNumber);
        List<BatchFileResponse> result = new ArrayList<>();

        // Always include IMAGE
        addFileResponse(result, records, "IMAGE");

        // COA and INVOICE only for Third Party
        if ("Third Party".equalsIgnoreCase(batchType)) {
            addFileResponse(result, records, "COA");
            addFileResponse(result, records, "INVOICE");
        }

        return result;
    }

    private void addFileResponse(List<BatchFileResponse> result, List<BatchDetails> records, String subType) {
        Optional<BatchDetails> match = records.stream()
                .filter(r -> subType.equalsIgnoreCase(r.getSubApplicationName()))
                .findFirst();

        if (match.isPresent() && match.get().getFileName() != null) {
            result.add(BatchFileResponse.builder()
                    .subApplicationName(subType)
                    .fileName(match.get().getFileName())
                    .filePath(match.get().getFilePath())
                    .hasFile(true)
                    .build());
        } else {
            result.add(BatchFileResponse.builder()
                    .subApplicationName(subType)
                    .hasFile(false)
                    .build());
        }
    }

    // ── CREATE / UPLOAD ───────────────────────────────────────────────────────
    @Transactional
    public void createBatch(
            String batchType, String productCode, String productName,
            String vendorCode, String vendorName, String batchNumber,
            String userId, String companyId, String locationId,
            String divisionName, String applicationName,
            MultipartFile coaFile, MultipartFile invoiceFile, MultipartFile imageFile) throws IOException {

        if ("Third Party".equalsIgnoreCase(batchType)) {
            handleFileUpload(batchType, "COA", coaFile, productCode, productName,
                    vendorCode, vendorName, batchNumber, userId, companyId, locationId, divisionName, applicationName);
            handleFileUpload(batchType, "INVOICE", invoiceFile, productCode, productName,
                    vendorCode, vendorName, batchNumber, userId, companyId, locationId, divisionName, applicationName);
            handleFileUpload(batchType, "IMAGE", imageFile, productCode, productName,
                    vendorCode, vendorName, batchNumber, userId, companyId, locationId, divisionName, applicationName);
        } else {
            // Own or Loan License — IMAGE only
            handleFileUpload(batchType, "IMAGE", imageFile, productCode, productName,
                    vendorCode, vendorName, batchNumber, userId, companyId, locationId, divisionName, applicationName);
        }
    }

    private void handleFileUpload(
            String batchType, String subAppType, MultipartFile file,
            String productCode, String productName, String vendorCode, String vendorName,
            String batchNumber, String userId, String companyId, String locationId,
            String divisionName, String applicationName) throws IOException {

        String fileName = null;
        String filePath = null;

        // Save file to disk if provided
        if (file != null && !file.isEmpty()) {
            String uploadDir = uploadBaseDir + "/" + applicationName + "/" + companyId + "/" + locationId
                    + "/" + batchType + "/" + productCode + "/" + batchNumber + "/" + subAppType + "/";
            File dir = new File(uploadDir);
            if (!dir.exists())
                dir.mkdirs();

            fileName = Objects.requireNonNull(file.getOriginalFilename())
                    .replaceAll("[&,]", "-");
            filePath = uploadDir + fileName;
            Path fullPath = Paths.get(uploadDir, fileName);
            Files.write(fullPath, file.getBytes());
        }

        // Upsert logic — matches original INSERT/UPDATE behavior
        Optional<BatchDetails> existing = batchRepo.findByBatchNumberAndProductCodeAndSubApplicationName(
                batchNumber, productCode, subAppType);

        if (existing.isPresent()) {
            // UPDATE only if a new file was actually uploaded
            if (fileName != null) {
                BatchDetails record = existing.get();
                record.setFileName(fileName);
                record.setFilePath(filePath);
                record.setCreatedBy(userId);
                record.setCreatedOn(LocalDateTime.now());
                batchRepo.save(record);
            }
        } else {
            // INSERT always (even if no file — row represents the batch entry)
            BatchDetails record = BatchDetails.builder()
                    .companyId(companyId)
                    .locationId(locationId)
                    .divisionName(divisionName)
                    .applicationName(applicationName)
                    .subApplicationName(subAppType)
                    .type(batchType)
                    .batchNumber(batchNumber)
                    .productCode(productCode)
                    .productName(productName)
                    .vendorCode(vendorCode)
                    .vendorName(vendorName)
                    .fileName(fileName)
                    .filePath(filePath)
                    .createdBy(userId)
                    .createdOn(LocalDateTime.now())
                    .build();
            batchRepo.save(record);
        }
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────
    public Page<BatchSearchResponse> searchBatch(
            String locationId, String subApplicationName, String type,
            String vendorCode, String productCode, String batchNumber,
            boolean isCreator, int page, int size) {

        // Non-creator users are restricted to their subApp only
        String subAppFilter = isCreator ? null : subApplicationName;

        Pageable pageable = PageRequest.of(page, size);
        Page<BatchDetails> results = batchRepo.searchBatch(
                locationId,
                subAppFilter,
                emptyToNull(type),
                emptyToNull(vendorCode),
                emptyToNull(productCode),
                emptyToNull(batchNumber),
                pageable);

        return results.map(b -> BatchSearchResponse.builder()
                .type(b.getType())
                .productCode(b.getProductCode())
                .productName(b.getProductName())
                .vendorCode(b.getVendorCode())
                .vendorName(b.getVendorName())
                .batchNumber(b.getBatchNumber())
                .subApplicationName(b.getSubApplicationName())
                .fileName(b.getFileName())
                .filePath(b.getFilePath())
                .createdBy(b.getCreatedBy())
                .createdOn(b.getCreatedOn())
                .build());
    }

    // ── FILTER DROPDOWNS ──────────────────────────────────────────────────────
    public BatchFilterResponse loadFilterData(String locationId, String type) {
        String typeFilter = emptyToNull(type);
        return BatchFilterResponse.builder()
                .vendorCodes(batchRepo.findDistinctVendorCodes(locationId, typeFilter))
                .productCodes(batchRepo.findDistinctProductCodes(locationId, typeFilter))
                .batchNumbers(batchRepo.findDistinctBatchNumbers(locationId, typeFilter))
                .build();
    }

    // ── REMOVE FILE ───────────────────────────────────────────────────────────
    @Transactional
    public void removeFile(String batchNumber, String fileName, String filePath) {
        // Clear from DB
        batchRepo.clearFile(batchNumber, fileName);

        // Delete from disk
        if (filePath != null && !filePath.isBlank()) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                // Log but don't fail — DB is already cleared
                System.err.println("Warning: Could not delete file from disk: " + filePath);
            }
        }
    }

    // ── HELPER ────────────────────────────────────────────────────────────────
    private String emptyToNull(String val) {
        return (val == null || val.isBlank()) ? null : val;
    }
}