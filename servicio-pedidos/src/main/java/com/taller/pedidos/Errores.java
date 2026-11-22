package com.taller.pedidos;

public final class Errores {
    private Errores() {}

    public static class ProductoNoEncontrado extends RuntimeException {
        public ProductoNoEncontrado(long id) { super("El producto " + id + " no existe"); }
    }

    public static class StockInsuficiente extends RuntimeException {
        public StockInsuficiente(long id) { super("Stock insuficiente para el producto " + id); }
    }

    public static class CatalogoNoDisponible extends RuntimeException {
        public CatalogoNoDisponible(Throwable causa) { super("servicio-catalogo no disponible", causa); }
    }
}
