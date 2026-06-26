package com.ipca.dms_api.controller;

import com.ipca.dms_api.dto.TruckLoadStuffResponse;
import com.ipca.dms_api.service.TruckLoadStuffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/dmsApi/truck-load-stuff")
public class TruckLoadStuffController {

    @Autowired
    private TruckLoadStuffService service;

    @GetMapping("/search-options")
    public ResponseEntity<List<String>> getSearchOptions(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String year) {
        return ResponseEntity.ok(service.getSearchOptions(companyId, locationId, year));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TruckLoadStuffResponse>> searchDocuments(
            @RequestParam(required = false) String invoiceNo,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String year) {
        return ResponseEntity.ok(service.searchDocuments(invoiceNo, companyId, locationId, year));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, String>> removeDocument(
            @RequestParam String invoiceNo,
            @RequestParam String fileName) {
        service.removeDocument(invoiceNo, fileName);
        return ResponseEntity.ok(Map.of("message", "Document removed successfully"));
    }

    @GetMapping("/scm-invoices")
    public ResponseEntity<List<String>> getScmInvoices(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String year) {
        return ResponseEntity.ok(service.getScmInvoices(companyId, locationId, year));
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createTruckLoadStuff(
            @RequestParam("invoiceNos") List<String> invoiceNos,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "companyId", required = false) String companyId,
            @RequestParam(value = "locationId", required = false) String locationId,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam(value = "division", required = false) String division,
            @RequestParam(value = "app", required = false) String app,
            @RequestParam(value = "createdBy", required = false) String createdBy) {
        try {
            boolean success = service.createTruckLoadStuff(invoiceNos, file, companyId, locationId, year, division, app, createdBy);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Created successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid input parameters"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error creating records"));
        }
    }

    @GetMapping("/view")
    public ResponseEntity<?> viewFile(
            @RequestParam("invoiceNo") String invoiceNo, 
            @RequestParam("fileName") String fileName) {
        try {
            File file = service.getFile(invoiceNo, fileName);
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
