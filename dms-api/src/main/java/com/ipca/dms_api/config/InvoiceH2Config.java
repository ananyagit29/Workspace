package com.ipca.dms_api.config;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InvoiceH2Config {

    @Bean
    public DataSource invoiceDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:invoice_documents;DB_CLOSE_DELAY=-1;MODE=Oracle")
                .username("sa")
                .password("")
                .build();
    }
}
