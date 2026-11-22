package com.taller.dbrouting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Verifica periodicamente cada replica. Una replica solo participa en las lecturas mientras
 * este en recuperacion Y recibiendo WAL; tras una caida se reincorpora unicamente despues de
 * N chequeos consecutivos correctos (app.db.healthy-after-successes).
 */
@Component
public class ReplicaHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(ReplicaHealthMonitor.class);

    private final DbRouter router;
    private final DbProperties props;

    public ReplicaHealthMonitor(DbRouter router, DbProperties props) {
        this.router = router;
        this.props = props;
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "${app.db.health-interval-ms:2000}")
    public void verificar() {
        for (DbNode nodo : router.replicas()) {
            boolean apta = nodo.probe(props);
            String cambio = nodo.apply(apta, props.healthyAfterSuccesses());
            if ("UP".equals(cambio)) {
                log.info("[DB-HEALTH] {} REINCORPORADA al grupo de lectura (cluster_name={})", nodo.name(), nodo.clusterName());
            } else if ("DOWN".equals(cambio)) {
                log.warn("[DB-HEALTH] {} EXCLUIDA de las lecturas: {}", nodo.name(), nodo.lastError());
            }
        }
    }
}
