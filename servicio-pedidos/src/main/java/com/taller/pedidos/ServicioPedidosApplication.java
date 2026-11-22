package com.taller.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(scanBasePackages = {"com.taller.pedidos", "com.taller.dbrouting"},
        exclude = DataSourceAutoConfiguration.class)
public class ServicioPedidosApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicioPedidosApplication.class, args);
    }
}
