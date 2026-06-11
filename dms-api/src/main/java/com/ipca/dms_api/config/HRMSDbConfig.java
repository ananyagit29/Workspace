package com.ipca.dms_api.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.ipca.dms2.repository.hrms",  // Create this package for HRMS repos if needed
    entityManagerFactoryRef = "hrmsEntityManagerFactory",
    transactionManagerRef = "hrmsTransactionManager"
)
public class HRMSDbConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hrms")
    public DataSourceProperties hrmsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource hrmsDataSource() {
        return hrmsDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean hrmsEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(hrmsDataSource())
                .packages("com.ipca.dms2.entity.hrms") // Create this for HRMS entities if using JPA
                .persistenceUnit("hrms")
                .properties(Map.of(
                        "hibernate.hbm2ddl.auto", "none",
                        "hibernate.show_sql", "true"))
                .build();
    }

    @Bean
    public PlatformTransactionManager hrmsTransactionManager(
            LocalContainerEntityManagerFactoryBean hrmsEntityManagerFactory) {
        return new JpaTransactionManager(java.util.Objects.requireNonNull(hrmsEntityManagerFactory.getObject()));
    }
}
