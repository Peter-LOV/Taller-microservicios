package com.taller.pedidos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Pedido(Long id, Long productoId, Integer cantidad, BigDecimal precioUnitario,
                     BigDecimal total, String estado, OffsetDateTime creadoEn) {}
