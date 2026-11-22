package com.taller.dbrouting;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Un nodo PostgreSQL (primario o replica) con su propio pool de conexiones y su estado de salud. */
public class DbNode {

    private static final String PROBE_SQL = """
            SELECT pg_is_in_recovery() AS en_recuperacion,
                   COALESCE((SELECT status = 'streaming' FROM pg_stat_wal_receiver LIMIT 1), false) AS streaming,
                   current_setting('cluster_name') AS cluster
            """;

    private final String name;
    private final String url;
    private final boolean replica;
    private final HikariDataSource pool;
    private final JdbcClient client;

    private volatile boolean healthy;
    private volatile int okStreak;
    private volatile boolean checkedOnce;
    private volatile String clusterName = "?";
    private volatile String lastError = "";
    private volatile Instant lastCheck;

    DbNode(String name, String url, boolean replica, DbProperties p) {
        this.name = name;
        this.url = url;
        this.replica = replica;
        this.healthy = !replica; // el primario siempre se considera disponible; las replicas deben demostrarlo
        HikariConfig c = new HikariConfig();
        c.setPoolName(name);
        c.setJdbcUrl(url);
        c.setUsername(p.username());
        c.setPassword(p.password());
        c.setSchema(p.schema());
        c.setMaximumPoolSize(p.poolSize());
        c.setMinimumIdle(1);
        c.setConnectionTimeout(p.connectionTimeoutMs());
        c.setValidationTimeout(Math.max(250, p.connectionTimeoutMs() - 500));
        c.setInitializationFailTimeout(-1); // la aplicacion arranca aunque el nodo este caido
        c.addDataSourceProperty("ApplicationName", p.applicationName());
        c.addDataSourceProperty("connectTimeout", "2");
        c.addDataSourceProperty("socketTimeout", "10");
        this.pool = new HikariDataSource(c);
        this.client = JdbcClient.create(pool);
    }

    public String name() { return name; }
    public JdbcClient client() { return client; }
    public boolean healthy() { return healthy; }

    /** Consulta de salud con una conexion aislada del pool. Una replica es "apta" si esta en recuperacion y recibiendo WAL. */
    boolean probe(DbProperties p) {
        Properties props = new Properties();
        props.setProperty("user", p.username());
        props.setProperty("password", p.password());
        props.setProperty("connectTimeout", "2");
        props.setProperty("socketTimeout", "3");
        props.setProperty("ApplicationName", p.applicationName() + "-health");
        lastCheck = Instant.now();
        try (Connection c = DriverManager.getConnection(url, props);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(PROBE_SQL)) {
            rs.next();
            clusterName = rs.getString("cluster");
            if (!rs.getBoolean("en_recuperacion")) {
                lastError = "el nodo no es una replica (pg_is_in_recovery = false)";
                return false;
            }
            if (p.requireStreaming() && !rs.getBoolean("streaming")) {
                lastError = "la replica aun no recibe WAL (no esta en streaming)";
                return false;
            }
            lastError = "";
            return true;
        } catch (SQLException e) {
            String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().replaceAll("\\s+", " ");
            lastError = m.length() > 160 ? m.substring(0, 160) : m;
            return false;
        }
    }

    /** Aplica el resultado de un chequeo. Devuelve "UP", "DOWN" o null si el estado no cambio. */
    String apply(boolean apt, int requiredSuccesses) {
        boolean first = !checkedOnce;
        checkedOnce = true;
        if (apt) {
            okStreak++;
            if (!healthy && (first || okStreak >= requiredSuccesses)) {
                healthy = true;
                return "UP";
            }
        } else {
            okStreak = 0;
            if (healthy || first) {
                healthy = false;
                return "DOWN";
            }
        }
        return null;
    }

    /** Marca la replica como no disponible tras un error de conexion en una consulta real. */
    void forceDown(String motivo) {
        healthy = false;
        okStreak = 0;
        lastError = motivo;
    }

    String lastError() { return lastError; }
    String clusterName() { return clusterName; }

    Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nombre", name);
        m.put("rol", replica ? "REPLICA (lectura)" : "PRIMARIO (escritura)");
        m.put("saludable", healthy);
        m.put("clusterNameReportado", replica ? clusterName : name);
        m.put("ultimoError", lastError);
        m.put("ultimoChequeo", lastCheck == null ? null : lastCheck.toString());
        return m;
    }

    void close() { pool.close(); }
}
