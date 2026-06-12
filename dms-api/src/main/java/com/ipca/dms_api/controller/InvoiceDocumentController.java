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
import java.util.List;

@RestController
@RequestMapping("/dmsApi/invoice")
public class InvoiceDocumentController {

    @Autowired
    private InvoiceDocumentService invoiceService;

    @GetMapping("/exists")
    public ResponseEntity<Boolean> exists(@RequestParam String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.exists(invoiceNumber));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(@RequestParam String query, @RequestParam(defaultValue = "false") boolean strict) {
        return ResponseEntity.ok(invoiceService.suggestInvoiceNumbers(query, strict));
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
            if (authentication == null || authentication.getName() == null) {
                throw new IllegalArgumentException("User is not authenticated properly.");
            }
            String userId = authentication.getName();
            return ResponseEntity.ok(invoiceService.saveInvoice(
                    invoiceNumber, companyId, locationId, divisionName, applicationName, userId, invoiceFile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/attach")
    public ResponseEntity<?> attach(
            Authentication authentication,
            @RequestParam String invoiceNumber,
            @RequestParam MultipartFile otherFile) {
        try {
            if (authentication == null || authentication.getName() == null) {
                throw new IllegalArgumentException("User is not authenticated properly.");
            }
            String userId = authentication.getName();
            return ResponseEntity.ok(invoiceService.attachOtherFile(invoiceNumber, userId, otherFile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/other-file/{invoiceNumber}")
    public ResponseEntity<?> replaceOtherFile(
            Authentication authentication,
            @PathVariable String invoiceNumber,
            @RequestParam MultipartFile newFile) {
        try {
            if (authentication == null || authentication.getName() == null) {
                throw new IllegalArgumentException("User is not authenticated properly.");
            }
            String userId = authentication.getName();
            return ResponseEntity.ok(invoiceService.replaceOtherFile(invoiceNumber, userId, newFile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/other-file/{invoiceNumber}")
    public ResponseEntity<?> deleteOtherFile(
            Authentication authentication,
            @PathVariable String invoiceNumber) {
        try {
            if (authentication == null || authentication.getName() == null) {
                throw new IllegalArgumentException("User is not authenticated properly.");
            }
            invoiceService.deleteOtherFile(invoiceNumber);
            return ResponseEntity.ok().build();
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
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "false") boolean strict) {
        return ResponseEntity.ok(invoiceService.search(invoiceNumber, locationId, page, size, strict));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id, @RequestParam(required = false) Long otherId) {
        return fileResponse(id, otherId, "inline");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, @RequestParam(required = false) Long otherId) {
        return fileResponse(id, otherId, "attachment");
    }

    private ResponseEntity<Resource> fileResponse(Long id, Long otherId, String disposition) {
        try {
            String filePathString;
            if (otherId != null) {
                filePathString = invoiceService.getOtherFilePath(otherId);
            } else {
                InvoiceDocumentResponse invoice = invoiceService.findById(id);
                filePathString = invoice.getFilePath();
            }
            Path path = Paths.get(filePathString);
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
