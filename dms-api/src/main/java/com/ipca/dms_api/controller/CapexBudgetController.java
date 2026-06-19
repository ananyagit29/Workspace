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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping
    public ResponseEntity<?> saveCapex(
            @RequestParam String budgetType,
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
                budgetType, budgetCode, companyId, locationId, divisionName, applicationName, year, userId, file);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving CapEx budget.");
        }
    }

    @PutMapping("/{budgetCode}/revise")
    public ResponseEntity<?> reviseCapex(
            @PathVariable String budgetCode,
            @RequestParam String userId,
            @RequestParam("file") MultipartFile file) {
        try {
            CapexBudgetResponse res = capexBudgetService.reviseCapex(budgetCode, userId, file);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error revising CapEx budget.");
        }
    }

    @DeleteMapping("/{budgetCode}")
    public ResponseEntity<?> removeCapex(@PathVariable String budgetCode) {
        try {
            capexBudgetService.removeCapex(budgetCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting CapEx budget.");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CapexBudgetResponse>> search(
            @RequestParam(required = false) String budgetType,
            @RequestParam(required = false) String budgetCode,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(capexBudgetService.search(budgetType, budgetCode, companyId, locationId, year, page, size));
    }

    @GetMapping("/{budgetCode}/view")
    public ResponseEntity<?> viewFile(@PathVariable String budgetCode) {
        try {
            File file = capexBudgetService.getFile(budgetCode);
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }
            InputStream in = new FileInputStream(file);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(in));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error accessing file.");
        }
    }
}
