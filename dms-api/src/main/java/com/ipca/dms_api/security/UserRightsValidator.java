package com.ipca.dms_api.security;

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
        
        List<Object[]> rawRights = userRightsRepository.findRawRightsByUserId(userId);

        boolean hasRight = rawRights.stream().anyMatch(row -> {
            String rCompanyId = (String) row[0];
            String rDivisionName = (String) row[1];
            String rLocationId = (String) row[2];
            String rApplicationName = (String) row[3];
            String rSubApplicationName = (String) row[4];
            String rAccessType = (String) row[5];

            return matches(companyId, rCompanyId) &&
                   matches(divisionName, rDivisionName) &&
                   matches(locationId, rLocationId) &&
                   matches(applicationName, rApplicationName) &&
                   matches(subApplicationName, rSubApplicationName) &&
                   matches(requiredAccessType, rAccessType);
        });

        if (!hasRight) {
            throw new AccessDeniedException("User cannot perform respective operation due to unsupported access rights.");
        }
    }

    // For operations like 'remove' that check across all subApps in an application
    public void requireAnySubAppRight(String userId, String companyId, String divisionName, String locationId,
                                      String applicationName, String requiredAccessType) {

        List<Object[]> rawRights = userRightsRepository.findRawRightsByUserId(userId);

        boolean hasRight = rawRights.stream().anyMatch(row -> {
            String rCompanyId = (String) row[0];
            String rDivisionName = (String) row[1];
            String rLocationId = (String) row[2];
            String rApplicationName = (String) row[3];
            String rAccessType = (String) row[5]; // index 5 is ACCESS_TYPE in the native query

            return matches(companyId, rCompanyId) &&
                   matches(divisionName, rDivisionName) &&
                   matches(locationId, rLocationId) &&
                   matches(applicationName, rApplicationName) &&
                   matches(requiredAccessType, rAccessType);
        });

        if (!hasRight) {
            System.out.println("AccessDenied! Required: App=" + applicationName + " Type=" + requiredAccessType + " for User=" + userId);
            System.out.println("Available rights:");
            rawRights.forEach(r -> {
                System.out.println("  App: " + r[3] + " Type: " + r[5] + " Comp: " + r[0] + " Loc: " + r[2] + " Div: " + r[1]);
            });
            throw new AccessDeniedException("User cannot perform respective operation due to unsupported access rights.");
        }
    }

    private boolean matches(String required, String actual) {
        if (required == null) return true;
        if (actual == null || actual.equalsIgnoreCase("ALL") || actual.equals("*")) return true;
        
        String normReq = required.replace(" ", "_").replace("&", "AND");
        String normAct = actual.replace(" ", "_").replace("&", "AND");
        
        return normReq.equalsIgnoreCase(normAct);
    }
}
