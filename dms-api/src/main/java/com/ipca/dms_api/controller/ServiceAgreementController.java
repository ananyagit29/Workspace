package com.ipca.dms_api.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/dmsApi/service-agreement")
public class ServiceAgreementController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${dms.upload.directory}")
    private String uploadDir;

    public ServiceAgreementController(@Qualifier("primaryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
    }

    // ─── GET DOCTOR DETAILS (from SCM via DB link) ───────────────────────
    @GetMapping("/doctor-details")
    public ResponseEntity<?> getDoctorDetails(@RequestParam String pan,
                                              @RequestParam String companyId,
                                              @RequestParam String userId) {
        try {
            // Exact query from the old system GetServiceAgreementDetailServlet
            String query = "Select h.code, h.name, a.infavour as in_favour_of " +
                    "from account_head@ipcascmdb h, account_head_infavour@ipcascmdb a " +
                    "Where a.company_code='1' and h.pan_no_1 = ? and nvl(final_approved,'N') = 'Y' " +
                    "and a.entity_code = '*' and a.control_code in ('4','2') and a.active='Y' " +
                    "and a.sr_no = (select max(b.sr_no) from account_head_infavour@ipcascmdb b " +
                    "where b.company_code = a.company_code and b.entity_code = a.entity_code and " +
                    "a.code = b.code and a.control_code = b.control_code and b.active ='Y' " +
                    "and nvl(b.final_approved,'N') = 'Y') and " +
                    "h.company_code=a.company_code and h.entity_code= a.entity_code " +
                    "and h.control_code = a.control_code and h.code = a.code and h.active = 'Y'";

            List<Map<String, Object>> doctors = jdbcTemplate.queryForList(query, pan);
            if (doctors.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Doctor Master Not Found"));
            }

            Map<String, Object> doc = doctors.get(0);
            Map<String, String> doctorInfo = new HashMap<>();
            doctorInfo.put("code", String.valueOf(doc.get("CODE")));
            doctorInfo.put("name", String.valueOf(doc.get("NAME")));
            doctorInfo.put("in_favour_of", String.valueOf(doc.get("IN_FAVOUR_OF")));

            // Get sub-divisions from HRMS via DB link, joined with DMS_USER_RIGHTS
            String subDivQuery = "SELECT DISTINCT h.code, h.name " +
                    "FROM hrms_live.subdivision@hrmsdrdb h, DMS_USER_RIGHTS d " +
                    "WHERE h.company_id = ? AND h.code = d.SUB_APPLICATION_NAME " +
                    "AND d.USER_ID = ? ORDER BY 2";

            List<Map<String, Object>> subDivs = jdbcTemplate.queryForList(subDivQuery, companyId, userId);
            List<Map<String, String>> subdivisions = new ArrayList<>();
            for (Map<String, Object> row : subDivs) {
                Map<String, String> sub = new HashMap<>();
                sub.put("code", String.valueOf(row.get("CODE")));
                sub.put("name", String.valueOf(row.get("NAME")));
                subdivisions.add(sub);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("doctor", doctorInfo);
            response.put("subdivisions", subdivisions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── GET SUBDIVISIONS (from HRMS via DB link) ───────────────────────
    @GetMapping("/subdivisions")
    public ResponseEntity<?> getSubdivisions(@RequestParam String companyId, @RequestParam String userId) {
        try {
            String subDivQuery = "SELECT DISTINCT h.code, h.name " +
                    "FROM hrms_live.subdivision@hrmsdrdb h, DMS_USER_RIGHTS d " +
                    "WHERE h.company_id = ? AND h.code = d.SUB_APPLICATION_NAME " +
                    "AND d.USER_ID = ? ORDER BY 2";

            List<Map<String, Object>> subDivs = jdbcTemplate.queryForList(subDivQuery, companyId, userId);
            List<Map<String, String>> subdivisions = new ArrayList<>();
            for (Map<String, Object> row : subDivs) {
                Map<String, String> sub = new HashMap<>();
                sub.put("code", String.valueOf(row.get("CODE")));
                sub.put("name", String.valueOf(row.get("NAME")));
                subdivisions.add(sub);
            }
            return ResponseEntity.ok(subdivisions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── GET EMPLOYEE DETAILS (from HRMS via DB link) ────────────────────
    @GetMapping("/employee-details")
    public ResponseEntity<?> getEmployeeDetails(@RequestParam String employeeId,
                                                @RequestParam String applicationName,
                                                @RequestParam String userId) {
        try {
            // Exact query from old system GetServiceAgreementDetailServlet
            String query = "SELECT 'H01', employee_id, first_name||' '||last_name as EMPLOYEE_NAME, " +
                    "responsibility_code, subdivision_code " +
                    "FROM hrms_live.pms_employee@hrmsdrdb " +
                    "WHERE COMPANY_ID='1' AND LOCATION_ID = 'H01' " +
                    "AND emp_status ='A' AND employee_id = ? " +
                    "UNION ALL " +
                    "SELECT '101', employee_id, first_name||' '||last_name as EMPLOYEE_NAME, " +
                    "responsibility_code, subdivision_code " +
                    "FROM hrms_live.pms_employee@hrmsdrdb " +
                    "WHERE COMPANY_ID='1' AND LOCATION_ID = '101' " +
                    "AND emp_status ='A' AND employee_id = ?";

            List<Map<String, Object>> employees = jdbcTemplate.queryForList(query,
                    employeeId, employeeId);

            if (employees.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "employee_id", (Object) null,
                        "employee_name", (Object) null,
                        "rc_code", (Object) null,
                        "subdivision_code", (Object) null
                ));
            }

            Map<String, Object> emp = employees.get(0);
            Map<String, Object> response = new HashMap<>();
            response.put("employee_id", emp.get("EMPLOYEE_ID"));
            response.put("employee_name", emp.get("EMPLOYEE_NAME"));
            response.put("rc_code", emp.get("RESPONSIBILITY_CODE"));
            response.put("subdivision_code", emp.get("SUBDIVISION_CODE"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── CHECK INTERFACE APP NO (duplicate check) ────────────────────────
    @GetMapping("/interface-details")
    public ResponseEntity<?> checkInterfaceDetails(@RequestParam String interfaceAppNo) {
        try {
            String query = "SELECT DOC_CODE, SUBDIVISION_NAME, DOCTOR_NAME, EMPLOYEE_NAME, CREATED_ON " +
                    "FROM DMS_SERVICE_AGREEMENT WHERE INTERFACE_APP_NO = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, interfaceAppNo);

            if (rows.isEmpty()) {
                // Available — no duplicate
                return ResponseEntity.ok(Map.of(
                        "doc_code", (Object) null,
                        "subdivision_name", (Object) null,
                        "doctor_name", (Object) null,
                        "employee_name", (Object) null,
                        "created_on", (Object) null
                ));
            }

            Map<String, Object> row = rows.get(0);
            return ResponseEntity.ok(row);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── CREATE SERVICE AGREEMENT ────────────────────────────────────────
    @PostMapping("/create")
    public ResponseEntity<?> createServiceAgreement(
            @RequestParam("companyId") String companyId,
            @RequestParam("locationId") String locationId,
            @RequestParam("divisionName") String divisionName,
            @RequestParam("applicationName") String applicationName,
            @RequestParam("financialYear") String financialYear,
            @RequestParam("subdivisionCode") String subdivisionCode,
            @RequestParam("subdivisionName") String subdivisionName,
            @RequestParam("doctorPan") String doctorPan,
            @RequestParam("doctorCode") String doctorCode,
            @RequestParam("doctorName") String doctorName,
            @RequestParam("inFavourOf") String inFavourOf,
            @RequestParam("employeeId") String employeeId,
            @RequestParam("employeeName") String employeeName,
            @RequestParam("rcCode") String rcCode,
            @RequestParam("interfaceAppNo") String interfaceAppNo,
            @RequestParam("cmeLogNo") String cmeLogNo,
            @RequestParam("amount") String amount,
            @RequestParam("eventFromDate") String eventFromDate,
            @RequestParam("eventToDate") String eventToDate,
            @RequestParam("eventName") String eventName,
            @RequestParam("createdBy") String createdBy,
            @RequestParam("file") MultipartFile file) {
        try {
            // Get next doc_code exactly like old system
            String docCodeQuery = "SELECT NVL(MAX(TO_NUMBER(doc_code)),0) + 1 AS doc_code " +
                    "FROM DMS_SERVICE_AGREEMENT WHERE financial_year = ? AND subdivision_code = ?";
            Integer docCode = jdbcTemplate.queryForObject(docCodeQuery, Integer.class, financialYear, subdivisionCode);
            if (docCode == null) docCode = 1;

            // Build upload directory: UPLOAD_DIR/app/com/div/loc/subappcode/finyear/doccode/
            String uploadPath = uploadDir + "/" + applicationName + "/" + companyId + "/" +
                    divisionName + "/" + locationId + "/" + subdivisionCode + "/" +
                    financialYear + "/" + docCode + "/";

            String fileName = "";
            String filePath = uploadPath;

            if (file != null && !file.isEmpty()) {
                fileName = file.getOriginalFilename();
                if (fileName != null) {
                    fileName = fileName.replace("&", "and").replace("'", "-");
                }
                filePath = uploadPath + fileName;

                Path targetDir = Paths.get(uploadPath);
                Files.createDirectories(targetDir);
                Path targetPath = targetDir.resolve(fileName);
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Insert exactly like old system
            String insertSql = "INSERT INTO DMS_SERVICE_AGREEMENT " +
                    "(COMPANY_ID, LOCATION_ID, DIVISION_NAME, APPLICATION_NAME, FINANCIAL_YEAR, " +
                    "SUBDIVISION_CODE, SUBDIVISION_NAME, DOC_CODE, " +
                    "DOCTOR_PAN, DOCTOR_CODE, DOCTOR_NAME, IN_FAVOUR_OF, " +
                    "EMPLOYEE_ID, EMPLOYEE_NAME, RC_CODE, " +
                    "INTERFACE_APP_NO, CME_LOG_NO, AMOUNT, " +
                    "EVENT_FROM_DATE, EVENT_TO_DATE, EVENT_NAME, " +
                    "CREATED_BY, CREATED_ON, FILE_NAME, FILE_PATH) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,TO_DATE(?,'YYYY-MM-DD'),TO_DATE(?,'YYYY-MM-DD'),?,?,?,?,?)";

            jdbcTemplate.update(insertSql,
                    companyId, locationId, divisionName, applicationName, financialYear,
                    subdivisionCode, subdivisionName, docCode,
                    doctorPan, doctorCode, doctorName, inFavourOf,
                    employeeId, employeeName, rcCode,
                    interfaceAppNo, cmeLogNo, amount,
                    eventFromDate, eventToDate, eventName,
                    createdBy, new Timestamp(System.currentTimeMillis()),
                    fileName, filePath);

            return ResponseEntity.ok(Map.of("message", "Doc Code " + docCode + " Created Successfully",
                    "docCode", docCode));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    // ─── SEARCH SERVICE AGREEMENT ────────────────────────────────────────
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("financialYear") String financialYear,
            @RequestParam("userId") String userId,
            @RequestParam(value = "subdivisionCode", required = false) String subdivisionCode,
            @RequestParam(value = "employeeId", required = false) String employeeId,
            @RequestParam(value = "createdDate", required = false) String createdDate,
            @RequestParam(value = "pan", required = false) String pan,
            @RequestParam(value = "paymentStatus", required = false) String paymentStatus) {
        try {
            // Exact query from old system SearchServiceAgreementServlet:
            // Join DMS_SERVICE_AGREEMENT with DMS_USER_RIGHTS to enforce security
            StringBuilder sql = new StringBuilder(
                    "SELECT DISTINCT SUBDIVISION_NAME, SUBDIVISION_CODE, DOC_CODE, DOCTOR_PAN, DOCTOR_CODE, DOCTOR_NAME, " +
                    "IN_FAVOUR_OF, EMPLOYEE_ID, EMPLOYEE_NAME, RC_CODE, INTERFACE_APP_NO, CME_LOG_NO, AMOUNT, " +
                    "EVENT_FROM_DATE, EVENT_TO_DATE, EVENT_NAME, VOUCHER_NO, VOUCHER_DATE, CHEQUE_NO, CHEQUE_DATE, " +
                    "CREATED_BY, CREATED_ON, FILE_NAME, FILE_PATH " +
                    "FROM DMS_SERVICE_AGREEMENT da, DMS_USER_RIGHTS d " +
                    "WHERE FINANCIAL_YEAR = ? " +
                    "AND d.USER_ID = ? " +
                    "AND d.SUB_APPLICATION_NAME = da.SUBDIVISION_CODE");

            List<Object> params = new ArrayList<>();
            params.add(financialYear);
            params.add(userId);

            if (StringUtils.hasText(subdivisionCode)) {
                sql.append(" AND SUBDIVISION_CODE = ?");
                params.add(subdivisionCode);
            }
            if (StringUtils.hasText(pan)) {
                sql.append(" AND DOCTOR_PAN = ?");
                params.add(pan);
            }
            if (StringUtils.hasText(employeeId)) {
                sql.append(" AND EMPLOYEE_ID = ?");
                params.add(employeeId);
            }
            if (StringUtils.hasText(createdDate)) {
                sql.append(" AND TRUNC(CREATED_ON) = TO_DATE(?, 'YYYY-MM-DD')");
                params.add(createdDate);
            }
            if (StringUtils.hasText(paymentStatus)) {
                if ("Paid".equalsIgnoreCase(paymentStatus)) {
                    sql.append(" AND CHEQUE_NO IS NOT NULL");
                } else if ("Unpaid".equalsIgnoreCase(paymentStatus)) {
                    sql.append(" AND CHEQUE_NO IS NULL");
                }
            }
            sql.append(" ORDER BY 1, 3 DESC");

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── REMOVE SERVICE AGREEMENT ────────────────────────────────────────
    @DeleteMapping("/remove")
    public ResponseEntity<?> remove(
            @RequestParam("docCode") Integer docCode,
            @RequestParam("subdivisionCode") String subdivisionCode,
            @RequestParam("fileName") String fileName,
            @RequestParam("filePath") String filePath) {
        try {
            // Delete the record
            String sql = "DELETE FROM DMS_SERVICE_AGREEMENT WHERE DOC_CODE = ? AND SUBDIVISION_CODE = ? AND FILE_NAME = ?";
            int deleted = jdbcTemplate.update(sql, docCode, subdivisionCode, fileName);

            if (deleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Record not found"));
            }

            // Try to delete the physical file
            try {
                Path path = Paths.get(filePath);
                Files.deleteIfExists(path);
            } catch (Exception ignored) {}

            return ResponseEntity.ok(Map.of("message", "Removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── VIEW FILE ───────────────────────────────────────────────────────
    @GetMapping("/view")
    public ResponseEntity<?> viewFile(@RequestParam String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "File not found"));
            }
            byte[] content = Files.readAllBytes(path);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "inline; filename=\"" + path.getFileName() + "\"")
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
