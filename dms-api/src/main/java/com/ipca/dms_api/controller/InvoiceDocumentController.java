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
    public ResponseEntity<Boolean> exists(
            @RequestParam String invoiceNumber,
            @RequestParam(required = false) String year) {
        return ResponseEntity.ok(invoiceService.exists(invoiceNumber, year));
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
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "false") boolean strict) {
        return ResponseEntity.ok(invoiceService.search(invoiceNumber, locationId, year, page, size, strict));
    }

    @GetMapping("/{invoiceNumber}/view")
    public ResponseEntity<Resource> view(@PathVariable String invoiceNumber, @RequestParam(required = false) String type) {
        return fileResponse(invoiceNumber, type, "inline");
    }

    @GetMapping("/{invoiceNumber}/download")
    public ResponseEntity<Resource> download(@PathVariable String invoiceNumber, @RequestParam(required = false) String type) {
        return fileResponse(invoiceNumber, type, "attachment");
    }

    private ResponseEntity<Resource> fileResponse(String invoiceNumber, String type, String disposition) {
        try {
            String filePathString = invoiceService.getFilePath(invoiceNumber, type);
            Path path = Paths.get(filePathString);
            Resource resource = new UrlResource(java.util.Objects.requireNonNull(path.toUri()));

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Test if we can actually open the stream. If AccessDeniedException occurs here, we can handle it cleanly.
            try (java.io.InputStream is = resource.getInputStream()) {
                // If it succeeds, the stream is closed immediately and we proceed.
            } catch (java.nio.file.AccessDeniedException e) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            } catch (Exception e) {
                e.printStackTrace();
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
