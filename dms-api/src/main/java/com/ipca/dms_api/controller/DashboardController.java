package com.ipca.dms_api.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.ipca.dms_api.repository.UserRightsRepository;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/dmsApi/dashboard")
public class DashboardController {


    @Autowired
    private UserRightsRepository userRightsRepository;

    @GetMapping("/getCompanies")
    public List<Object[]> getCompanies(Authentication authentication) {
        String userId = authentication.getName();
        return userRightsRepository.findCompany(userId);
    }

    @GetMapping("/getDivisions")
    public List<Object[]> getDivisions(
            Authentication authentication,
            @RequestParam String companyId) {

        String userId = authentication.getName();
        return userRightsRepository.findDivision(userId, companyId);
    }

    @GetMapping("/getLocations")
    public List<Object[]> getLocations(
            Authentication authentication,
            @RequestParam String companyId,
            @RequestParam String divisionId) {

        String userId = authentication.getName();
        return userRightsRepository.findLocation(userId, companyId, divisionId);
    }

    @GetMapping("/getApplications")
    public List<Object[]> getApplications(
            Authentication authentication,
            @RequestParam String companyId,
            @RequestParam String divisionId,
            @RequestParam String locationId) {

        String userId = authentication.getName();
        return userRightsRepository.findApplication(userId, companyId, divisionId, locationId);
    }

    @GetMapping("/getYears")
    public String getYears(@RequestParam String applicationName) {
        return userRightsRepository.findStartYearByApplication(applicationName);
    }

    @GetMapping("/getSubApplications")
    public List<Object[]> getSubApplications(
            Authentication authentication,
            @RequestParam String companyId,
            @RequestParam String divisionId,
            @RequestParam String locationId,
            @RequestParam String applicationName) {

        String userId = authentication.getName();
        return userRightsRepository.findSubApplicationNames(userId, companyId, divisionId, locationId, applicationName);
    }

    @GetMapping("/getModules")
    public List<Object[]> getModules(
            Authentication authentication,
            @RequestParam String companyId,
            @RequestParam String divisionId,
            @RequestParam String locationId,
            @RequestParam String applicationName,
            @RequestParam String subApplicationName) {

        String userId = authentication.getName();
        return userRightsRepository.findModules(userId, companyId, divisionId, locationId, applicationName,
                subApplicationName);
    }

}