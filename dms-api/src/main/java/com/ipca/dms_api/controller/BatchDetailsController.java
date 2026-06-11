package com.ipca.dms_api.controller;

import com.ipca.dms_api.dto.*;
import com.ipca.dms_api.service.BatchDetailsService;
import com.ipca.dms_api.service.ZipMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/dmsApi/batch")
public class BatchDetailsController {

    @Autowired
    private BatchDetailsService batchService;
    @Autowired
    private ZipMailService zipMailService;

    // ── GET PRODUCT DETAILS ───────────────────────────────────────────────────
    @GetMapping("/getProductDetails")
    public ResponseEntity<?> getProductDetails(
            @RequestParam String batchType,
            @RequestParam String productCode) {
        try {
            ProductDetailsResponse res = batchService.getProductDetails(batchType, productCode);
            if (res == null)
                return ResponseEntity.ok().body("No matching records found.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ── GET FILE NAMES FOR A BATCH ────────────────────────────────────────────
    @GetMapping("/getFileNames")
    public ResponseEntity<List<BatchFileResponse>> getFileNames(
            @RequestParam String productCode,
            @RequestParam String batchType,
            @RequestParam String batchNumber) {
        return ResponseEntity.ok(batchService.getFileNames(productCode, batchType, batchNumber));
    }

    // ── CREATE BATCH ──────────────────────────────────────────────────────────
    @PostMapping("/save")
    public ResponseEntity<?> saveBatch(
            Authentication authentication,
            @RequestParam String batchType,
            @RequestParam String productCode,
            @RequestParam String productName,
            @RequestParam String vendorCode,
            @RequestParam String vendorName,
            @RequestParam String batchNumber,
            @RequestParam String companyId,
            @RequestParam String locationId,
            @RequestParam String divisionName,
            @RequestParam String applicationName,
            @RequestParam(required = false) MultipartFile coaFile,
            @RequestParam(required = false) MultipartFile invoiceFile,
            @RequestParam(required = false) MultipartFile imageFile) {
        try {
            String userId = authentication.getName();
            batchService.createBatch(
                    batchType, productCode, productName, vendorCode, vendorName,
                    batchNumber, userId, companyId, locationId, divisionName,
                    applicationName, coaFile, invoiceFile, imageFile);
            return ResponseEntity.ok("Batch saved successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ── SEARCH (paginated) ────────────────────────────────────────────────────
    @GetMapping("/search")
    public ResponseEntity<Page<BatchSearchResponse>> search(
            Authentication authentication,
            @RequestParam String locationId,
            @RequestParam(required = false) String subApplicationName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String vendorCode,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {

        boolean isCreator = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("Creator"));

        return ResponseEntity.ok(batchService.searchBatch(
                locationId, subApplicationName, type,
                vendorCode, productCode, batchNumber,
                isCreator, page, size));
    }

    // ── LOAD FILTER DROPDOWNS ─────────────────────────────────────────────────
    @GetMapping("/loadFilterData")
    public ResponseEntity<BatchFilterResponse> loadFilterData(
            @RequestParam String locationId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(batchService.loadFilterData(locationId, type));
    }

    // ── REMOVE FILE ───────────────────────────────────────────────────────────
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFile(
            @RequestParam String batchNumber,
            @RequestParam String fileName,
            @RequestParam String filePath) {
        try {
            batchService.removeFile(batchNumber, fileName, filePath);
            return ResponseEntity.ok("File removed successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ── VIEW FILE ─────────────────────────────────────────────────────────────
    @GetMapping("/view")
    public ResponseEntity<org.springframework.core.io.Resource> viewFile(
            @RequestParam String filePath) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(java.util.Objects.requireNonNull(path.toUri()));

            if (!resource.exists() || !resource.isReadable())
                return ResponseEntity.notFound().build();

            String contentType = java.nio.file.Files.probeContentType(path);
            if (contentType == null)
                contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "inline; filename=\"" + path.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── ZIP & MAIL ────────────────────────────────────────────────────────────
    record ZipMailRequest(List<String> filePaths, List<String> recipients) {
    }

    @PostMapping("/zipAndMail")
    public ResponseEntity<?> zipAndMail(@RequestBody ZipMailRequest body) {
        try {
            if (body.filePaths() == null || body.filePaths().isEmpty())
                return ResponseEntity.badRequest().body("No file paths provided.");
            if (body.recipients() == null || body.recipients().isEmpty())
                return ResponseEntity.badRequest().body("Recipient email is required.");
            if (body.filePaths().size() > 10)
                return ResponseEntity.badRequest().body("Maximum 10 files allowed.");
            if (body.recipients().size() > 5)
                return ResponseEntity.badRequest().body("Maximum 5 recipients allowed.");

            zipMailService.zipAndMail(body.filePaths(), body.recipients());
            return ResponseEntity.ok("Files zipped and mailed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}