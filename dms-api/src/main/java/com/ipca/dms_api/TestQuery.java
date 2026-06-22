package com.ipca.dms_api;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import com.ipca.dms_api.service.CapexBudgetService;
import org.springframework.data.domain.Page;

public class TestQuery {
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(DmsApiApplication.class, args);
        CapexBudgetService service = ctx.getBean(CapexBudgetService.class);
        Page<?> p = service.search("1", null, null, "1", "101", "2022-2023", 0, 7);
        System.out.println("TOTAL ELEMENTS: " + p.getTotalElements());
        System.out.println("CONTENT SIZE: " + p.getContent().size());
        System.exit(0);
    }
}
