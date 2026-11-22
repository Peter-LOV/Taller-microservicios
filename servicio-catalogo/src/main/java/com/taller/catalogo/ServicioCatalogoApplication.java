package com.taller.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// Se excluye el DataSource unico de Spring Boot: la persistencia la gestiona DbRouter (primario + replicas).
@SpringBootApplication(scanBasePackages = {"com.taller.catalogo", "com.taller.dbrouting"},
        exclude = DataSourceAutoConfiguration.class)
public class ServicioCatalogoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicioCatalogoApplication.class, args);
    }
}
