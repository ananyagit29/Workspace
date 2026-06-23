package com.ipca.dms_api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/dmsApi/dbinfo")
public class DbInfoController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DbInfoController(@Qualifier("primaryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
    }

    @GetMapping("/dms")
    public List<Map<String, Object>> getDmsSchema() {
        return jdbcTemplate.queryForList("SELECT column_name, data_type FROM user_tab_cols WHERE table_name = 'DMS_CAPEX_BUDGET'");
    }

    @GetMapping("/scm")
    public List<Map<String, Object>> getScmSchema() {
        return jdbcTemplate.queryForList("SELECT column_name, data_type FROM all_tab_cols@IPCASCMDB WHERE table_name = 'FA_ASSETS_BUDGET_HEADER'");
    }

    @Autowired
    private com.ipca.dms_api.service.CapexBudgetService capexBudgetService;

    @GetMapping("/testCodes")
    public Object testCodes() {
        try {
            return capexBudgetService.getBudgetCodes("IT", "1", "101", "2025-2026");
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "trace", e.toString());
        }
    }

    @GetMapping("/testTypes")
    public Object testTypes() {
        try {
            return capexBudgetService.getBudgetTypes("1", "101", "2025-2026");
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "trace", e.toString());
        }
    }

    @GetMapping("/testQuery")
    public Object testQuery() {
        try {
            return jdbcTemplate.queryForList("SELECT DISTINCT BUDGET_TYPE FROM DMS_CAPEX_BUDGET");
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "trace", e.toString());
        }
    }
}
