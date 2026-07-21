package com.ipca.dms_api.repository;

import com.ipca.dms_api.entity.ServiceAgreementEntity;
import com.ipca.dms_api.entity.ServiceAgreementId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ServiceAgreementRepository extends JpaRepository<ServiceAgreementEntity, ServiceAgreementId> {

    @Query("SELECT COALESCE(MAX(s.docCode), 0) + 1 FROM ServiceAgreementEntity s WHERE s.companyId = :companyId AND s.locationId = :locationId AND s.financialYear = :financialYear AND s.subdivisionCode = :subdivisionCode")
    Integer getNextDocCode(@Param("companyId") String companyId,
                           @Param("locationId") String locationId,
                           @Param("financialYear") String financialYear,
                           @Param("subdivisionCode") String subdivisionCode);

    boolean existsByInterfaceAppNo(String interfaceAppNo);
    
    // Dynamic query for search will be handled by Criteria API or QueryDSL, or simple JPA method.
    // For simplicity, we can do a JPQL with optional params or just use Specifications/JdbcTemplate.
}
