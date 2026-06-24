package com.ipca.dms_api.controller;

import com.ipca.dms_api.dto.SupplierCustomerResponse;
import com.ipca.dms_api.service.SupplierCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dmsApi/supplier-customer")
public class SupplierCustomerController {

    @Autowired
    private SupplierCustomerService service;

    @GetMapping("/search-options")
    public ResponseEntity<List<Map<String, Object>>> getSearchOptions(
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId) {
        return ResponseEntity.ok(service.getSearchAccountOptions(accountType, companyId, locationId));
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
            @RequestParam String accountCode,
            @RequestParam String fileName) {
        service.removeDocument(accountCode, fileName);
        return ResponseEntity.ok(Map.of("message", "Document removed successfully"));
    }
}
