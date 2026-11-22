package com.taller.pedidos;

import jakarta.validation.constraints.Positive;

public final class Solicitudes {
    private Solicitudes() {}

    public record CrearPedido(@Positive long productoId, @Positive int cantidad) {}
}
