package com.taller.dbrouting;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuracion del enrutamiento (prefijo app.db en application.yml). */
@ConfigurationProperties(prefix = "app.db")
public record DbProperties(
        String username,
        String password,
        @DefaultValue("public") String schema,
        @DefaultValue("app") String applicationName,
        @DefaultValue("5") int poolSize,
        @DefaultValue("1500") int connectionTimeoutMs,
        @DefaultValue("2000") int healthIntervalMs,
        @DefaultValue("2") int healthyAfterSuccesses,
        @DefaultValue("true") boolean requireStreaming,
        Node primary,
        List<Node> replicas) {

    public record Node(String name, String url) {}
}
