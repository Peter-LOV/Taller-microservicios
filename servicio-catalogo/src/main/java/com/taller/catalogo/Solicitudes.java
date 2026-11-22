package com.taller.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public final class Solicitudes {
    private Solicitudes() {}

    public record CrearProducto(@NotBlank String nombre, @NotNull @Positive BigDecimal precio, @PositiveOrZero int stock) {}

    public record Reservar(@Positive int cantidad) {}
}
