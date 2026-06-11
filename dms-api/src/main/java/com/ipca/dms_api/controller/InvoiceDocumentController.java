package com.ipca.dms_api.controller;

import com.ipca.dms_api.dto.InvoiceDocumentResponse;
import com.ipca.dms_api.service.InvoiceDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/dmsApi/invoice")
public class InvoiceDocumentController {

    @Autowired
    private InvoiceDocumentService invoiceService;

    @GetMapping("/exists")
    public ResponseEntity<Boolean> exists(@RequestParam String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.exists(invoiceNumber));
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(
            Authentication authentication,
            @RequestParam String invoiceNumber,
            @RequestParam String companyId,
            @RequestParam String locationId,
            @RequestParam String divisionName,
            @RequestParam String applicationName,
            @RequestParam MultipartFile invoiceFile) {
        try {
            String userId = authentication == null ? "SYSTEM" : authentication.getName();
            return ResponseEntity.ok(invoiceService.saveInvoice(
                    invoiceNumber, companyId, locationId, divisionName, applicationName, userId, invoiceFile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<InvoiceDocumentResponse>> search(
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(invoiceService.search(invoiceNumber, locationId, page, size));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id) {
        return fileResponse(id, "inline");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return fileResponse(id, "attachment");
    }

    private ResponseEntity<Resource> fileResponse(Long id, String disposition) {
        try {
            InvoiceDocumentResponse invoice = invoiceService.findById(id);
            Path path = Paths.get(invoice.getFilePath());
            Resource resource = new UrlResource(java.util.Objects.requireNonNull(path.toUri()));

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/pdf";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename=\"" + path.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
