package com.ipca.dms_api.controller;

import com.ipca.dms_api.dto.SupplierCustomerResponse;
import com.ipca.dms_api.service.SupplierCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.ipca.dms_api.security.UserRightsValidator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dmsApi/supplier-customer")
public class SupplierCustomerController {

    @Autowired
    private SupplierCustomerService service;

    @Autowired
    private UserRightsValidator userRightsValidator;

    @GetMapping("/search-options")
    public ResponseEntity<List<Map<String, Object>>> getSearchOptions(
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId) {
        return ResponseEntity.ok(service.getSearchAccountOptions(accountType, companyId, locationId));
    }

    @GetMapping("/scm-options")
    public ResponseEntity<List<Map<String, Object>>> getScmSearchOptions(@RequestParam(required = false) String accountType) {
        return ResponseEntity.ok(service.getScmAccountOptions(accountType));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SupplierCustomerResponse>> searchDocuments(
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.searchDocuments(accountType, accountCode, companyId, locationId, page, size));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, String>> removeDocument(
            Authentication authentication,
            @RequestParam String accountCode,
            @RequestParam String fileName) {
        userRightsValidator.requireAnySubAppRight(authentication.getName(), null, null, null, "Supplier and Customer", "Remove");
        service.removeDocument(accountCode, fileName);
        return ResponseEntity.ok(Map.of("message", "Document removed successfully"));
    }
    @PostMapping("/add-file")
    public ResponseEntity<?> saveSupplierCustomer(
            @RequestParam String accountType,
            @RequestParam String accountCode,
            @RequestParam String accountName,
            @RequestParam String companyId,
            @RequestParam String locationId,
            @RequestParam String divisionName,
            @RequestParam String applicationName,
            @RequestParam String userId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            userRightsValidator.requireRight(authentication.getName(), companyId, divisionName, locationId, applicationName, null, "Creator");
            SupplierCustomerResponse res = service.saveDocument(
                accountType, accountCode, accountName, companyId, locationId, divisionName, applicationName, userId, file);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving Supplier & Customer document: " + e.getMessage());
        }
    }

    @GetMapping("/view")
    public ResponseEntity<?> viewFile(@RequestParam("accountCode") String accountCode, @RequestParam("fileName") String fileName) {
        try {
            File file = service.getFile(accountCode, fileName);
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }
            InputStream in = new FileInputStream(file);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(new InputStreamResource(in));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error accessing file.");
        }
    }
}
