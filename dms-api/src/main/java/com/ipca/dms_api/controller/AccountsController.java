package com.ipca.dms_api.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ipca.dms_api.service.AccountsService;

@RestController
@RequestMapping("/dmsApi/accounts")
public class AccountsController {

    private final AccountsService accountsService;

    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    // ── 1. Daybooks dropdown ────────────────────────────────────────────────

    @GetMapping("/daybooks")
    public ResponseEntity<List<Map<String, Object>>> getDaybooks() {
        return ResponseEntity.ok(accountsService.getDaybooks());
    }

    // ── 2. Doc list (un-uploaded from SCM) for Create dropdown ──────────────

    @GetMapping("/doc-list")
    public ResponseEntity<List<Map<String, Object>>> getDocList(
            @RequestParam String locationId,
            @RequestParam String daybookCode,
            @RequestParam String year,
            @RequestParam String month) {
        return ResponseEntity.ok(accountsService.getDocList(locationId, daybookCode, year, month));
    }

    // ── 3. Doc details for Create ───────────────────────────────────────────

    @GetMapping("/doc-details")
    public ResponseEntity<Map<String, Object>> getDocDetails(
            @RequestParam String companyId,
            @RequestParam String locationId,
            @RequestParam String daybookCode,
            @RequestParam String docCode,
            @RequestParam String year,
            @RequestParam String month) {
        Map<String, Object> details = accountsService.getDocDetails(companyId, locationId, daybookCode, docCode, year, month);
        if (details == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(details);
    }

    // ── 4. Check if daybook requires Account Name / Bill Number ─────────────

    @GetMapping("/field-required")
    public ResponseEntity<Boolean> isFieldRequired(@RequestParam String daybookCode) {
        return ResponseEntity.ok(accountsService.isFieldRequired(daybookCode));
    }

    // ── 5. Upload document ──────────────────────────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<String> upload(
            @RequestParam String companyId,
            @RequestParam String locationId,
            @RequestParam String divisionName,
            @RequestParam String applicationName,
            @RequestParam String financialYear,
            @RequestParam String daybookCode,
            @RequestParam String daybookName,
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) String accountName,
            @RequestParam String docCode,
            @RequestParam(required = false) String docDate,
            @RequestParam(required = false) String docYear,
            @RequestParam(required = false) String billNumber,
            @RequestParam(required = false) String billDate,
            @RequestParam(defaultValue = "0") double tranAmount,
            @RequestParam String docMonth,
            @RequestParam String createdBy,
            @RequestParam("file") MultipartFile file) {
        try {
            String msg = accountsService.uploadDocument(companyId, locationId, divisionName,
                    applicationName, financialYear, daybookCode, daybookName,
                    accountCode, accountName, docCode, docDate, docYear,
                    billNumber, billDate, tranAmount, docMonth, createdBy, file);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    // ── 6. Search uploaded documents ────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam String locationId,
            @RequestParam String daybookCode,
            @RequestParam String year,
            @RequestParam String month,
            @RequestParam(required = false) String docCode) {
        return ResponseEntity.ok(accountsService.search(locationId, daybookCode, year, month, docCode));
    }

    // ── 7. Remove document ──────────────────────────────────────────────────

    @DeleteMapping("/remove")
    public ResponseEntity<String> remove(
            @RequestParam String daybookCode,
            @RequestParam(required = false) String docMonth,
            @RequestParam(required = false) String docYear,
            @RequestParam String docCode,
            @RequestParam String filename) {
        accountsService.removeDocument(daybookCode, docMonth, docYear, docCode, filename);
        return ResponseEntity.ok("Document removed");
    }

    // ── 8. Party names for Report ───────────────────────────────────────────

    @GetMapping("/party-names")
    public ResponseEntity<List<Map<String, Object>>> getPartyNames(
            @RequestParam String daybookCode,
            @RequestParam String financialYear,
            @RequestParam String locationId) {
        return ResponseEntity.ok(accountsService.getPartyNames(daybookCode, financialYear, locationId));
    }

    // ── 9. Missing documents for Report ─────────────────────────────────────

    @GetMapping("/missing")
    public ResponseEntity<List<Map<String, Object>>> getMissing(
            @RequestParam String locationId,
            @RequestParam String daybookCode,
            @RequestParam String year) {
        return ResponseEntity.ok(accountsService.getMissingDocuments(locationId, daybookCode, year));
    }

    // ── 10. Report: filtered docs (party/account code wise) ─────────────────

    @GetMapping("/report")
    public ResponseEntity<List<Map<String, Object>>> getReport(
            @RequestParam String locationId,
            @RequestParam String daybookCode,
            @RequestParam String year,
            @RequestParam String fromMonth,
            @RequestParam String toMonth,
            @RequestParam(required = false) String accName,
            @RequestParam(required = false) String accCode,
            @RequestParam(required = false) String amountMoreThan) {
        return ResponseEntity.ok(accountsService.getReportDocuments(locationId, daybookCode,
                year, fromMonth, toMonth, accName, accCode, amountMoreThan));
    }

    // ── 11. View / Download file ────────────────────────────────────────────

    @GetMapping("/view-file")
    public ResponseEntity<Resource> viewFile(
            @RequestParam String daybookCode,
            @RequestParam String docCode,
            @RequestParam String fileName) {
        try {
            File file = accountsService.getFile(daybookCode, docCode, fileName);
            if (file == null || !file.exists()) return ResponseEntity.notFound().build();

            Path path = file.toPath();
            Resource resource = new UrlResource(path.toUri());

            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "application/pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
