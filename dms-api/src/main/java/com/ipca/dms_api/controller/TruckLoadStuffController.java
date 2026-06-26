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
