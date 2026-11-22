package com.taller.pedidos;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {

    /**
     * @LoadBalanced: la URL "http://servicio-catalogo/..." se resuelve por NOMBRE LOGICO consultando Eureka;
     * Spring Cloud LoadBalancer elige la instancia (round-robin). No hay IPs ni puertos fijos.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(1000);
        f.setReadTimeout(3000);
        return new RestTemplate(f);
    }
}
