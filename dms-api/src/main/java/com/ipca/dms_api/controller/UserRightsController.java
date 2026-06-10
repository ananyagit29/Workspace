package com.ipca.dms_api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ipca.dms_api.dto.PageResponse;
import com.ipca.dms_api.dto.UserRightsDTO;
import com.ipca.dms_api.service.UserRightsService;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/dmsApi/userRights")
public class UserRightsController {

    @Autowired
    private UserRightsService userRightsService;

    @GetMapping("/list")
    public PageResponse<UserRightsDTO> rightsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {

        // Map DTO field names to Entity fields
        String sortField = switch (sortBy) {
            case "userId" -> "id.userId";
            case "companyId" -> "id.companyId";
            case "divisionName" -> "id.divisionName";
            case "locationId" -> "id.locationId";
            case "applicationName" -> "id.applicationName";
            case "subApplicationName" -> "id.subApplicationName";
            case "module" -> "id.module";
            case "accessType" -> "id.accessType";
            case "companyName" -> "companyName";
            case "locationName" -> "locationName";
            default -> "id.userId";
        };

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserRightsDTO> rightsPage = userRightsService.searchUserRights(
                search != null ? search.trim() : "",
                pageable);

        return new PageResponse<>(
                rightsPage.getContent(),
                rightsPage.getNumber(),
                rightsPage.getSize(),
                rightsPage.getTotalElements(),
                rightsPage.getTotalPages(),
                rightsPage.isFirst(),
                rightsPage.isLast());
    }
}