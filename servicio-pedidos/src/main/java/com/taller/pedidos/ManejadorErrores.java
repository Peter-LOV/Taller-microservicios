package com.taller.pedidos;

import com.taller.pedidos.Errores.CatalogoNoDisponible;
import com.taller.pedidos.Errores.ProductoNoEncontrado;
import com.taller.pedidos.Errores.StockInsuficiente;
import java.util.Map;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ProductoNoEncontrado.class)
    public ResponseEntity<Map<String, String>> noEncontrado(ProductoNoEncontrado e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(StockInsuficiente.class)
    public ResponseEntity<Map<String, String>> sinStock(StockInsuficiente e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CatalogoNoDisponible.class)
    public ResponseEntity<Map<String, String>> catalogoCaido(CatalogoNoDisponible e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", e.getMessage(), "accion", "El pedido NO fue creado; reintente en unos segundos"));
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<Map<String, String>> bdCaida(DataAccessResourceFailureException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Base de datos no disponible"));
    }
}
