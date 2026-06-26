package com.ipca.dms_api.service;

import com.ipca.dms_api.dto.TruckLoadStuffResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;
import java.util.stream.Collectors;

@Service
public class TruckLoadStuffService {

    private final JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Value("${dms.upload.directory}")
    private String uploadDir;

    public TruckLoadStuffService(@Qualifier("primaryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
    }

    private String clean(String val) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : null;
    }

    public List<String> getSearchOptions(String companyId, String locationId, String year) {
        StringBuilder sql = new StringBuilder(
                "select d.INVOICE_NO " +
                "from DMS_TRUCK_LOAD_STUFF d, hrms_live.pms_master_entity@hrmsdrdb h " +
                "where d.Location_Id = h.code "
        );
        List<Object> params = new ArrayList<>();

        if (clean(year) != null) {
            sql.append(" AND d.FINANCIAL_YEAR = ?");
            params.add(clean(year));
        }
        if (clean(companyId) != null) {
            sql.append(" AND d.COMPANY_ID = ?");
            params.add(clean(companyId));
        }
        if (clean(locationId) != null) {
            sql.append(" AND d.LOCATION_ID = ?");
            params.add(clean(locationId));
        }

        sql.append(" order by d.INVOICE_NO desc");

        try {
            String query = Objects.requireNonNull(sql.toString());
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, params.toArray());
            return results.stream()
                    .map(row -> (String) row.get("INVOICE_NO"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching TLS search options: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public List<TruckLoadStuffResponse> searchDocuments(String invoiceNo, String companyId, String locationId, String year) {
        StringBuilder sql = new StringBuilder(
                "SELECT INVOICE_NO, FILE_NAME, CREATED_BY, CREATED_ON, FILE_PATH " +
                "FROM DMS_TRUCK_LOAD_STUFF WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (clean(year) != null) {
            sql.append(" AND FINANCIAL_YEAR = ?");
            params.add(clean(year));
        }
        if (clean(companyId) != null) {
            sql.append(" AND COMPANY_ID = ?");
            params.add(clean(companyId));
        }
        if (clean(locationId) != null) {
            sql.append(" AND LOCATION_ID = ?");
            params.add(clean(locationId));
        }
        if (clean(invoiceNo) != null) {
            sql.append(" AND INVOICE_NO = ?");
            params.add(clean(invoiceNo));
        }

        sql.append(" ORDER BY CREATED_ON DESC");

        try {
            String query = Objects.requireNonNull(sql.toString());
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, params.toArray());
            List<TruckLoadStuffResponse> responses = new ArrayList<>();
            for (Map<String, Object> row : results) {
                TruckLoadStuffResponse res = new TruckLoadStuffResponse();
                res.setInvoiceNo((String) row.get("INVOICE_NO"));
                res.setFileName((String) row.get("FILE_NAME"));
                res.setCreatedBy((String) row.get("CREATED_BY"));
                res.setCreatedOn((Date) row.get("CREATED_ON"));
                res.setFilePath((String) row.get("FILE_PATH"));
                responses.add(res);
            }
            return responses;
        } catch (Exception e) {
            System.err.println("Error searching TLS documents: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public File getFile(String invoiceNo, String fileName) throws IOException {
        String sql = "SELECT FILE_PATH FROM DMS_TRUCK_LOAD_STUFF WHERE INVOICE_NO = ? AND FILE_NAME = ?";
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, invoiceNo, fileName);
            if (!results.isEmpty()) {
                String path = (String) results.get(0).get("FILE_PATH");
                if (path != null && !path.trim().isEmpty()) {
                    File file = new File(path);
                    if (file.exists()) return file;
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving file path from DB: " + e.getMessage());
        }

        // Fallback
        java.nio.file.Path filePath = Paths.get(uploadDir, "TRUCK_LOAD_STUFF", invoiceNo, fileName);
        return filePath.toFile();
    }

    public void removeDocument(String invoiceNo, String fileName) {
        try {
            File f = getFile(invoiceNo, fileName);
            if (f != null && f.exists()) {
                Files.deleteIfExists(f.toPath());
            }
            String sql = "DELETE FROM DMS_TRUCK_LOAD_STUFF WHERE INVOICE_NO = ? AND FILE_NAME = ?";
            jdbcTemplate.update(sql, invoiceNo, fileName);
        } catch (Exception e) {
            System.err.println("Error removing TLS document: " + e.getMessage());
        }
    }

    public List<String> getScmInvoices(String companyId, String locationId, String year) {
        String frm_dt = "";
        String to_dt = "";
        if (year != null && !year.isEmpty()) {
            String[] fin_year = year.split("-");
            if (fin_year.length == 2) {
                frm_dt = "01-APR-" + fin_year[0];
                to_dt = "31-MAR-" + fin_year[1];
            }
        }
        
        String query = "SELECT ltrim(rtrim(REPLACE(h.FINAL_PREFIX, '/', '-') || h.FINAL_INV_DOC_CODE || h.suffix)) as Invoice_no "
                + "FROM ipcaprod.ea_custom_invoice_header@ipcascmdb h "
                + "inner join ipcaprod.ea_custom_invoice_detail@ipcascmdb d on h.doc_code = d.doc_code "
                + "where h.division_code=d.division_code "
                + "and h.entity_code = d.entity_code "
                + "and h.transaction_id = d.transaction_id "
                + "and h.doc_date=d.doc_date "
                + "and h.STATUS <> '4' "
                + "and h.DOC_DATE between ? and ? "
                + "and d.pl_entity_code= ? "
                + "and h.FINAL_INV_DOC_CODE is not null "
                + "and not exists (select 'x' from DMS_TRUCK_LOAD_STUFF "
                + "where location_id = d.pl_entity_code "
                + "and INVOICE_NO = ltrim(rtrim(REPLACE(h.FINAL_PREFIX, '/', '-') || h.FINAL_INV_DOC_CODE || h.suffix))) "
                + "group by h.FINAL_PREFIX,h.FINAL_INV_DOC_CODE,h.suffix "
                + "order by to_number(h.FINAL_INV_DOC_CODE) desc";

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, frm_dt, to_dt, locationId);
            return results.stream()
                    .map(row -> (String) row.get("INVOICE_NO"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching SCM invoices: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean createTruckLoadStuff(List<String> invoiceNos, MultipartFile file, String companyId, String locationId, String year, String division, String app, String createdBy) throws IOException {
        if (invoiceNos == null || invoiceNos.isEmpty() || file == null || file.isEmpty()) {
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "uploaded_file.pdf";
        }
        
        // Save the file physically once in a common location
        java.nio.file.Path targetDir = Paths.get(uploadDir, "TRUCK_LOAD_STUFF", "Shared");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        
        String savedFileName = System.currentTimeMillis() + "_" + originalFilename.replaceAll("[\\\\s\\\\/]", "_");
        java.nio.file.Path targetPath = targetDir.resolve(savedFileName);
        file.transferTo(Objects.requireNonNull(targetPath.toFile()));
        
        String absoluteFilePath = targetPath.toAbsolutePath().toString();

        String insertSql = "INSERT INTO DMS_TRUCK_LOAD_STUFF " +
                           "(INVOICE_NO, FILE_NAME, CREATED_BY, CREATED_ON, FILE_PATH, FINANCIAL_YEAR, COMPANY_ID, LOCATION_ID, DIVISION_NAME, APPLICATION_NAME) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                           
        for (String invoiceNo : invoiceNos) {
            jdbcTemplate.update(insertSql, invoiceNo, originalFilename, createdBy, new Date(), absoluteFilePath, year, companyId, locationId, division, app);
        }
        
        return true;
    }
}
