package com.ipca.dms_api.security;

import com.ipca.dms_api.entity.UserRights;
import com.ipca.dms_api.repository.UserRightsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserRightsValidator {

    @Autowired
    private UserRightsRepository userRightsRepository;

    public void requireRight(String userId, String companyId, String divisionName, String locationId,
                             String applicationName, String subApplicationName, String requiredAccessType) {
        
        List<UserRights> userRights = userRightsRepository.findByUserId(userId);

        boolean hasRight = userRights.stream().anyMatch(r ->
                (companyId == null || companyId.equalsIgnoreCase(r.getId().getCompanyId())) &&
                (divisionName == null || divisionName.equalsIgnoreCase(r.getId().getDivisionName())) &&
                (locationId == null || locationId.equalsIgnoreCase(r.getId().getLocationId())) &&
                (applicationName == null || applicationName.equalsIgnoreCase(r.getId().getApplicationName())) &&
                (subApplicationName == null || subApplicationName.equalsIgnoreCase(r.getId().getSubApplicationName())) &&
                requiredAccessType.equalsIgnoreCase(r.getId().getAccessType())
        );

        if (!hasRight) {
            throw new AccessDeniedException("User cannot perform respective operation due to unsupported access rights.");
        }
    }

    // For operations like 'remove' that check across all subApps in an application
    public void requireAnySubAppRight(String userId, String companyId, String divisionName, String locationId,
                                      String applicationName, String requiredAccessType) {

        List<UserRights> userRights = userRightsRepository.findByUserId(userId);

        boolean hasRight = userRights.stream().anyMatch(r ->
                (companyId == null || companyId.equalsIgnoreCase(r.getId().getCompanyId())) &&
                (divisionName == null || divisionName.equalsIgnoreCase(r.getId().getDivisionName())) &&
                (locationId == null || locationId.equalsIgnoreCase(r.getId().getLocationId())) &&
                (applicationName == null || applicationName.equalsIgnoreCase(r.getId().getApplicationName())) &&
                requiredAccessType.equalsIgnoreCase(r.getId().getAccessType())
        );

        if (!hasRight) {
            throw new AccessDeniedException("User cannot perform respective operation due to unsupported access rights.");
        }
    }
}
