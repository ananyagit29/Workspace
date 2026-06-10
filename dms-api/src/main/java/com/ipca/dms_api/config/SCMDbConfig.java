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
@EnableJpaRepositories(basePackages = "com.ipca.dms2.repository.scm", // repos for SCM DB
        entityManagerFactoryRef = "scmEntityManagerFactory", transactionManagerRef = "scmTransactionManager")
public class SCMDbConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.scm")
    public DataSourceProperties scmDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource scmDataSource() {
        return scmDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean scmEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(scmDataSource())
                .packages("com.ipca.aims.entity.scm")
                .persistenceUnit("scm")
                .properties(Map.of(
                        "hibernate.hbm2ddl.auto", "none", // don’t touch schema
                        "hibernate.show_sql", "true"))
                .build();
    }

    @Bean
    public PlatformTransactionManager scmTransactionManager(
            LocalContainerEntityManagerFactoryBean scmEntityManagerFactory) {
        return new JpaTransactionManager(scmEntityManagerFactory.getObject());
    }
}
