package com.ipca.dms_api.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ipca.dms_api.dto.CapexBudgetResponse;
import com.ipca.dms_api.service.CapexBudgetService;

@RestController
@RequestMapping("/dmsApi/capex")
public class CapexBudgetController {

    @Autowired
    private CapexBudgetService capexBudgetService;

    @GetMapping("/types")
    public ResponseEntity<List<String>> getBudgetTypes(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String year) {
        return ResponseEntity.ok(capexBudgetService.getBudgetTypes(companyId, locationId, year));
    }

    @GetMapping("/codes")
    public ResponseEntity<List<String>> getBudgetCodes(
            @RequestParam(required = false) String budgetType,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String year) {
        return ResponseEntity.ok(capexBudgetService.getBudgetCodes(budgetType, companyId, locationId, year));
    }

    @GetMapping("/search-codes")
    public ResponseEntity<List<String>> getSearchBudgetCodes(
            @RequestParam(required = false) String budgetType,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String year) {
        return ResponseEntity.ok(capexBudgetService.getSearchBudgetCodes(budgetType, companyId, locationId, year));
    }

    @GetMapping("/revisions")
    public ResponseEntity<List<String>> getRevisions(
            @RequestParam String budgetCode) {
        return ResponseEntity.ok(capexBudgetService.getRevisions(budgetCode));
    }

    @PostMapping
    public ResponseEntity<?> saveCapex(
            @RequestParam String budgetType,
            @RequestParam String transactionId,
            @RequestParam String budgetCode,
            @RequestParam String companyId,
            @RequestParam String locationId,
            @RequestParam String divisionName,
            @RequestParam String applicationName,
            @RequestParam String year,
            @RequestParam String userId,
            @RequestParam("file") MultipartFile file) {
        try {
            CapexBudgetResponse res = capexBudgetService.saveCapex(
                budgetType, transactionId, budgetCode, companyId, locationId, divisionName, applicationName, year, userId, file);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving CapEx budget.");
        }
    }

    @PutMapping("/revise")
    public ResponseEntity<?> reviseCapex(@RequestParam("budgetCode") String budgetCode, @RequestParam("userId") String userId, @RequestParam("file") MultipartFile file) {
        try {
            CapexBudgetResponse res = capexBudgetService.reviseCapex(budgetCode, userId, file);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error revising document: " + e.getMessage() + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "none"));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeCapex(@RequestParam("budgetCode") String budgetCode, @RequestParam(required = false) String revision) {
        try {
            capexBudgetService.removeCapex(budgetCode, revision);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting CapEx budget.");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CapexBudgetResponse>> searchCapex(
            @RequestParam(required = false) String budgetType,
            @RequestParam(required = false) String budgetCode,
            @RequestParam(required = false) String revision,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(capexBudgetService.search(budgetType, budgetCode, revision, companyId, locationId, year, page, size));
    }

    @GetMapping("/view")
    public ResponseEntity<?> viewFile(@RequestParam("budgetCode") String budgetCode, @RequestParam(required = false) String revision) {
        try {
            File file = capexBudgetService.getFile(budgetCode, revision);
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

    @GetMapping("/debug")
    public ResponseEntity<?> debugTable() {
        return ResponseEntity.ok(capexBudgetService.debugTable());
    }
}
