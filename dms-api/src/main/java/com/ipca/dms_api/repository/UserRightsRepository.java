
package com.ipca.dms_api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ipca.dms_api.dto.UserRightsDTO;
import com.ipca.dms_api.entity.UserRights;
import com.ipca.dms_api.entity.UserRightsId;

@Repository
public interface UserRightsRepository extends JpaRepository<UserRights, UserRightsId> {

   @Query("SELECT r FROM UserRights r WHERE r.id.userId = :userId")
   List<UserRights> findByUserId(@Param("userId") String userId);

   @Query("SELECT DISTINCT ur.id.companyId, ur.companyName FROM UserRights ur " +
         "WHERE ur.id.userId = :userId " +
         "ORDER BY ur.companyName")
   List<Object[]> findCompany(@Param("userId") String userId);

   @Query("SELECT DISTINCT ur.id.divisionName FROM UserRights ur " +
         "WHERE ur.id.userId = :userId " +
         "AND ur.id.companyId = :companyId " +
         "ORDER BY ur.id.divisionName")
   List<Object[]> findDivision(@Param("userId") String userId,
         @Param("companyId") String companyId);

   @Query("SELECT DISTINCT ur.id.locationId, ur.locationName FROM UserRights ur " +
         "WHERE ur.id.userId = :userId " +
         "AND ur.id.companyId = :companyId " +
         "AND ur.id.divisionName = :divisionName " +
         "ORDER BY ur.locationName")
   List<Object[]> findLocation(@Param("userId") String userId,
         @Param("companyId") String companyId,
         @Param("divisionName") String divisionName);

   @Query("SELECT DISTINCT ur.id.applicationName FROM UserRights ur " +
         "WHERE ur.id.userId = :userId " +
         "AND ur.id.companyId = :companyId " +
         "AND ur.id.divisionName = :divisionName " +
         "AND ur.id.locationId = :locationId " +
         "ORDER BY ur.id.applicationName")
   List<Object[]> findApplication(@Param("userId") String userId,
         @Param("companyId") String companyId,
         @Param("divisionName") String divisionName,
         @Param("locationId") String locationId);

   @Query("SELECT DISTINCT ur.id.subApplicationName FROM UserRights ur " +
         "WHERE ur.id.userId = :userId " +
         "AND ur.id.companyId = :companyId " +
         "AND ur.id.divisionName = :divisionName " +
         "AND ur.id.locationId = :locationId " +
         "AND ur.id.applicationName = :applicationName " +
         "ORDER BY ur.id.subApplicationName")
   List<Object[]> findSubApplicationNames(@Param("userId") String userId,
         @Param("companyId") String companyId,
         @Param("divisionName") String divisionName,
         @Param("locationId") String locationId,
         @Param("applicationName") String applicationName);

   @Query("SELECT DISTINCT ur.id.module FROM UserRights ur " +
         "WHERE ur.id.userId = :userId " +
         "AND ur.id.companyId = :companyId " +
         "AND ur.id.divisionName = :divisionName " +
         "AND ur.id.locationId = :locationId " +
         "AND ur.id.applicationName = :applicationName " +
         "AND ur.id.subApplicationName = :subApplicationName " +
         "ORDER BY ur.id.module")
   List<Object[]> findModules(@Param("userId") String userId,
         @Param("companyId") String companyId,
         @Param("divisionName") String divisionName,
         @Param("locationId") String locationId,
         @Param("applicationName") String applicationName,
         @Param("subApplicationName") String subApplicationName);

   // Native query (no change needed)
   @Query(value = "SELECT parameter_value FROM dms_general_parameters " +
         "WHERE application_name = :applicationName " +
         "AND parameter_name = 'Year'", nativeQuery = true)
   String findStartYearByApplication(@Param("applicationName") String applicationName);

   // Native pagination (no change needed)
   @Query(value = "SELECT DISTINCT USER_ID, COMPANY_ID, DIVISION_NAME, LOCATION_ID, APPLICATION_NAME, SUB_APPLICATION_NAME, MODULE, ACCESS_TYPE, COMPANY_NAME, LOCATION_NAME "
         +
         "FROM DMS_USER_RIGHTS " +
         "WHERE UPPER(USER_ID) LIKE %:search% " +
         "OR UPPER(COMPANY_NAME) LIKE %:search% " +
         "OR UPPER(LOCATION_NAME) LIKE %:search% " +
         "OR UPPER(APPLICATION_NAME) LIKE %:search% " +
         "OR UPPER(MODULE) LIKE %:search%", countQuery = "SELECT COUNT(*) FROM (SELECT DISTINCT USER_ID, COMPANY_ID, DIVISION_NAME, LOCATION_ID, APPLICATION_NAME, SUB_APPLICATION_NAME, MODULE, ACCESS_TYPE, COMPANY_NAME, LOCATION_NAME "
               +
               "FROM DMS_USER_RIGHTS " +
               "WHERE UPPER(USER_ID) LIKE %:search% " +
               "OR UPPER(COMPANY_NAME) LIKE %:search% " +
               "OR UPPER(LOCATION_NAME) LIKE %:search% " +
               "OR UPPER(APPLICATION_NAME) LIKE %:search% " +
               "OR UPPER(MODULE) LIKE %:search%)", nativeQuery = true)
   Page<Object[]> searchUserRightsNative(@Param("search") String search, Pageable pageable);

   // JPQL search (FIXED)
   @Query("""
         SELECT r FROM UserRights r
         WHERE LOWER(r.id.userId) LIKE %:search%
            OR LOWER(r.companyName) LIKE %:search%
            OR LOWER(r.locationName) LIKE %:search%
            OR LOWER(r.id.module) LIKE %:search%
         """)
   Page<UserRights> searchRights(@Param("search") String search, Pageable pageable);

   // DTO search (FIXED)
   @Query("""
             SELECT new com.ipca.dms_api.dto.UserRightsDTO(
                 r.id.userId,
                 r.id.companyId,
                 r.id.divisionName,
                 r.id.locationId,
                 r.id.applicationName,
                 r.id.subApplicationName,
                 r.id.module,
                 r.id.accessType,
                 r.companyName,
                 r.locationName
             )
             FROM UserRights r
             WHERE LOWER(r.id.userId) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.companyName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.locationName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.id.module) LIKE LOWER(CONCAT('%', :search, '%'))
         """)
   Page<UserRightsDTO> searchRightsDTO(@Param("search") String search, Pageable pageable);
}
