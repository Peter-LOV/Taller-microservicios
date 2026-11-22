package com.taller.catalogo;

import com.taller.dbrouting.DbRouter;
import com.taller.dbrouting.DbRouter.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;

@Repository
public class ProductoRepository {

    private static final String COLUMNAS = "id, nombre, precio, stock";
    private static final RowMapper<Producto> MAPPER = (rs, i) ->
            new Producto(rs.getLong("id"), rs.getString("nombre"), rs.getBigDecimal("precio"), rs.getInt("stock"));

    private final DbRouter db;

    public ProductoRepository(DbRouter db) {
        this.db = db;
    }

    /** Lectura ordinaria: se reparte entre las replicas. */
    public Result<List<Producto>> listar() {
        return db.read("SELECT " + COLUMNAS + " FROM productos ORDER BY id", s -> s.query(MAPPER).list());
    }

    /** fuerte=true -> lectura critica al primario; fuerte=false -> replica. */
    public Result<Optional<Producto>> buscar(long id, boolean fuerte) {
        String sql = "SELECT " + COLUMNAS + " FROM productos WHERE id = :id";
        Function<StatementSpec, Optional<Producto>> op = s -> s.param("id", id).query(MAPPER).optional();
        return fuerte ? db.readPrimary(sql, op) : db.read(sql, op);
    }

    public Result<Producto> crear(String nombre, BigDecimal precio, int stock) {
        return db.write("INSERT INTO productos (nombre, precio, stock) VALUES (:n, :p, :s) RETURNING " + COLUMNAS,
                s -> s.param("n", nombre).param("p", precio).param("s", stock).query(MAPPER).single());
    }

    /** Descuento atomico de stock. Vacio si el producto no existe o no hay stock suficiente. */
    public Result<Optional<Producto>> reservar(long id, int cantidad) {
        return db.write("UPDATE productos SET stock = stock - :c WHERE id = :id AND stock >= :c RETURNING " + COLUMNAS,
                s -> s.param("c", cantidad).param("id", id).query(MAPPER).optional());
    }
}
