package com.taller.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rutas por NOMBRE LOGICO (lb://). El Gateway consulta Eureka y Spring Cloud LoadBalancer
 * reparte las solicitudes (round-robin) entre las instancias registradas.
 * Las rutas se definen en Java para no depender del prefijo de propiedades, que cambio en Spring Cloud 2025.0.
 */
@Configuration
public class RutasConfig {

    @Bean
    public RouteLocator rutas(RouteLocatorBuilder builder) {
        return builder.routes()
                // /api/catalogo/productos -> servicio-catalogo: /catalogo/productos
                .route("catalogo", r -> r.path("/api/catalogo/**")
                        .filters(f -> f.stripPrefix(1).retry(c -> c.setRetries(2)))
                        .uri("lb://servicio-catalogo"))
                // /api/pedidos/5 -> servicio-pedidos: /pedidos/5
                .route("pedidos", r -> r.path("/api/pedidos/**")
                        .filters(f -> f.stripPrefix(1).retry(c -> c.setRetries(2)))
                        .uri("lb://servicio-pedidos"))
                .build();
    }
}
