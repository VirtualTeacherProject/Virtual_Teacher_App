package com.MarianFinweFeanor.Virtual_Teacher.Config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@PropertySource("classpath:application.properties")
public class HibernateConfig {
// this section commented out because of database migration

//    private final String dbUrl, dbUsername, dbPassword;
//
//    @Autowired
//    public HibernateConfig(Environment env) {
//        this.dbUrl = env.getProperty("database.url");
//        this.dbUsername = env.getProperty("database.username");
//        this.dbPassword = env.getProperty("database.password");
//    }

//    @Bean(name = "entityManagerFactory")
//    public LocalSessionFactoryBean sessionFactory() {
//        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
//        sessionFactory.setDataSource(datasource());
//        sessionFactory.setPackagesToScan("com.MarianFinweFeanor.Virtual_Teacher");
//        sessionFactory.setHibernateProperties(hibernateProperties());
//        return sessionFactory;
//    }
//
//
//    @Bean
//    public DataSource datasource() {
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
//        dataSource.setUrl(dbUrl);
//        dataSource.setUsername(dbUsername);
//        dataSource.setPassword(dbPassword);
//        return dataSource;
//    }

//    public Properties hibernateProperties() {
//        Properties hibernateProperties = new Properties();
//        hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
//        return hibernateProperties;
//    }
}
