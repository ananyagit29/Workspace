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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dmsApi/config")
public class ConfigController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ConfigController(@Qualifier("primaryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource));
    }

    @GetMapping("/yearParameters")
    public List<Map<String, Object>> getYearParameters() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT APPLICATION_NAME, PARAMETER_VALUE FROM DMS_GENERAL_PARAMETERS WHERE PARAMETER_NAME = 'Year'"
        );
        
        return rows.stream().map(row -> {
            String appName = (String) row.get("APPLICATION_NAME");
            String paramVal = (String) row.get("PARAMETER_VALUE");
            int startYear = 0;
            try {
                if (paramVal != null) {
                    startYear = Integer.parseInt(paramVal.trim());
                }
            } catch (NumberFormatException ignored) {}
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("applicationName", appName != null ? appName : "");
            result.put("startYear", startYear);
            return result;
        }).filter(map -> (Integer) map.get("startYear") > 0).collect(Collectors.toList());
    }
}
