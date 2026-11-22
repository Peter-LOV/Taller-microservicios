package com.taller.pedidos;

import com.taller.dbrouting.DbRouter;
import com.taller.dbrouting.DbRouter.Result;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRepository {

    private static final String COLUMNAS = "id, producto_id, cantidad, precio_unitario, total, estado, creado_en";
    private static final RowMapper<Pedido> MAPPER = (rs, i) -> new Pedido(
            rs.getLong("id"), rs.getLong("producto_id"), rs.getInt("cantidad"),
            rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("total"), rs.getString("estado"),
            rs.getObject("creado_en", OffsetDateTime.class));

    private final DbRouter db;

    public PedidoRepository(DbRouter db) {
        this.db = db;
    }

    public Result<Pedido> crear(long productoId, int cantidad, BigDecimal precio, BigDecimal total) {
        return db.write("INSERT INTO pedidos (producto_id, cantidad, precio_unitario, total) "
                        + "VALUES (:pid, :c, :p, :t) RETURNING " + COLUMNAS,
                s -> s.param("pid", productoId).param("c", cantidad).param("p", precio).param("t", total)
                        .query(MAPPER).single());
    }

    public Result<List<Pedido>> listar() {
        return db.read("SELECT " + COLUMNAS + " FROM pedidos ORDER BY id DESC LIMIT 50", s -> s.query(MAPPER).list());
    }

    public Result<Optional<Pedido>> buscar(long id, boolean fuerte) {
        String sql = "SELECT " + COLUMNAS + " FROM pedidos WHERE id = :id";
        Function<StatementSpec, Optional<Pedido>> op = s -> s.param("id", id).query(MAPPER).optional();
        return fuerte ? db.readPrimary(sql, op) : db.read(sql, op);
    }
}
