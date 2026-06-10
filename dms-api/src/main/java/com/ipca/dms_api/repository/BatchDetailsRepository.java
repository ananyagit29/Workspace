package com.ipca.dms_api.repository;

import com.ipca.dms_api.entity.BatchDetails;
import com.ipca.dms_api.entity.BatchDetailsId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchDetailsRepository extends JpaRepository<BatchDetails, BatchDetailsId> {

    // ── CREATE: find existing record for upsert ───────────────────────────────
    Optional<BatchDetails> findByBatchNumberAndProductCodeAndSubApplicationName(
            String batchNumber,
            String productCode,
            String subApplicationName);

    // ── CREATE: get uploaded files for a batch (IMAGE → COA → INVOICE order) ─
    @Query("SELECT b FROM BatchDetails b " +
            "WHERE b.productCode = :productCode " +
            "AND b.type = :type " +
            "AND b.batchNumber = :batchNumber " +
            "ORDER BY CASE b.subApplicationName " +
            "WHEN 'IMAGE' THEN 0 WHEN 'COA' THEN 1 WHEN 'INVOICE' THEN 2 ELSE 3 END")
    List<BatchDetails> findFilesByBatch(
            @Param("productCode") String productCode,
            @Param("type") String type,
            @Param("batchNumber") String batchNumber);

    // ── SEARCH: paginated with optional filters ───────────────────────────────
    @Query("SELECT b FROM BatchDetails b " +
            "WHERE b.locationId = :locationId " +
            "AND b.fileName IS NOT NULL " +
            "AND (:subApplicationName IS NULL OR b.subApplicationName = :subApplicationName) " +
            "AND (:type IS NULL OR b.type = :type) " +
            "AND (:vendorCode IS NULL OR b.vendorCode = :vendorCode) " +
            "AND (:productCode IS NULL OR b.productCode = :productCode) " +
            "AND (:batchNumber IS NULL OR b.batchNumber LIKE %:batchNumber%) " +
            "ORDER BY b.type, b.batchNumber")
    Page<BatchDetails> searchBatch(
            @Param("locationId") String locationId,
            @Param("subApplicationName") String subApplicationName,
            @Param("type") String type,
            @Param("vendorCode") String vendorCode,
            @Param("productCode") String productCode,
            @Param("batchNumber") String batchNumber,
            Pageable pageable);

    // ── FILTER DROPDOWNS ──────────────────────────────────────────────────────
    @Query("SELECT DISTINCT b.vendorCode FROM BatchDetails b " +
            "WHERE b.locationId = :locationId " +
            "AND b.fileName IS NOT NULL " +
            "AND (:type IS NULL OR b.type = :type) " +
            "ORDER BY b.vendorCode")
    List<String> findDistinctVendorCodes(
            @Param("locationId") String locationId,
            @Param("type") String type);

    @Query("SELECT DISTINCT b.productCode FROM BatchDetails b " +
            "WHERE b.locationId = :locationId " +
            "AND b.fileName IS NOT NULL " +
            "AND (:type IS NULL OR b.type = :type) " +
            "ORDER BY b.productCode")
    List<String> findDistinctProductCodes(
            @Param("locationId") String locationId,
            @Param("type") String type);

    @Query("SELECT DISTINCT b.batchNumber FROM BatchDetails b " +
            "WHERE b.locationId = :locationId " +
            "AND b.fileName IS NOT NULL " +
            "AND (:type IS NULL OR b.type = :type) " +
            "ORDER BY b.batchNumber")
    List<String> findDistinctBatchNumbers(
            @Param("locationId") String locationId,
            @Param("type") String type);

    // ── REMOVE: clear file info only, keep the row ────────────────────────────
    @Modifying
    @Query("UPDATE BatchDetails b SET b.fileName = NULL, b.filePath = NULL " +
            "WHERE b.batchNumber = :batchNumber " +
            "AND b.fileName = :fileName")
    int clearFile(
            @Param("batchNumber") String batchNumber,
            @Param("fileName") String fileName);
}