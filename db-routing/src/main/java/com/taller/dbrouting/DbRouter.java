package com.taller.dbrouting;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Component;

/**
 * Enrutador de persistencia (politica documentada en el README):
 *  - write():        siempre al PRIMARIO.
 *  - readPrimary():  lecturas criticas (consistencia fuerte) al PRIMARIO.
 *  - read():         lecturas ordinarias, round-robin entre las REPLICAS SANAS.
 *                    Si una replica falla en plena consulta se excluye y se reintenta en la otra.
 *                    Si no queda ninguna replica sana, se degrada al primario (disponibilidad antes que aislamiento).
 */
@Component
public class DbRouter {

    /** Valor devuelto junto con el nombre del nodo que atendio la operacion. */
    public record Result<T>(T valor, String nodo) {}

    private static final Logger log = LoggerFactory.getLogger(DbRouter.class);

    private final DbNode primary;
    private final List<DbNode> replicas;
    private final AtomicLong turno = new AtomicLong();

    public DbRouter(DbProperties p) {
        this.primary = new DbNode(p.primary().name(), p.primary().url(), false, p);
        List<DbNode> r = new ArrayList<>();
        if (p.replicas() != null) {
            for (DbProperties.Node n : p.replicas()) {
                r.add(new DbNode(n.name(), n.url(), true, p));
            }
        }
        this.replicas = List.copyOf(r);
    }

    public <T> Result<T> write(String sql, Function<StatementSpec, T> op) {
        log.info("[DB] WRITE         nodo={} sql={}", primary.name(), compacto(sql));
        return new Result<>(op.apply(primary.client().sql(sql)), primary.name());
    }

    public <T> Result<T> readPrimary(String sql, Function<StatementSpec, T> op) {
        log.info("[DB] READ-CRITICA  nodo={} sql={}", primary.name(), compacto(sql));
        return new Result<>(op.apply(primary.client().sql(sql)), primary.name());
    }

    public <T> Result<T> read(String sql, Function<StatementSpec, T> op) {
        List<DbNode> sanas = replicas.stream().filter(DbNode::healthy).toList();
        long inicio = turno.getAndIncrement();
        for (int i = 0; i < sanas.size(); i++) {
            DbNode nodo = sanas.get((int) ((inicio + i) % sanas.size()));
            try {
                T valor = op.apply(nodo.client().sql(sql));
                log.info("[DB] READ          nodo={} sql={}", nodo.name(), compacto(sql));
                return new Result<>(valor, nodo.name());
            } catch (DataAccessResourceFailureException e) {
                nodo.forceDown(String.valueOf(e.getMostSpecificCause().getMessage()));
                log.warn("[DB] READ falla en nodo={} -> excluida y se reintenta con otra replica", nodo.name());
            }
        }
        log.warn("[DB] READ sin replicas sanas -> degradacion al primario");
        return readPrimary(sql, op);
    }

    List<DbNode> replicas() { return replicas; }

    public List<Map<String, Object>> status() {
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(primary.status());
        replicas.forEach(r -> out.add(r.status()));
        return out;
    }

    private static String compacto(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @PreDestroy
    void close() {
        primary.close();
        replicas.forEach(DbNode::close);
    }
}
