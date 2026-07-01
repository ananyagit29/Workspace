package com.ipca.dms_api.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
@RestController
public class DebugController {
    @Autowired private JdbcTemplate jdbcTemplate;
    @GetMapping("/debug/rights")
    public List<Map<String, Object>> getRights() {
        return jdbcTemplate.queryForList("SELECT DISTINCT APPLICATION_NAME FROM DMS_USER_RIGHTS");
    }
}
