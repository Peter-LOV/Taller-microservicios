package com.taller.gateway;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Registra a que instancia concreta fue enviada cada solicitud (evidencia para P02 y P03). */
@Component
public class LogDestinoFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LogDestinoFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).doFinally(signal -> {
            URI destino = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            log.info("[GW] {} {} -> {} [{}]", exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(), destino, exchange.getResponse().getStatusCode());
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
