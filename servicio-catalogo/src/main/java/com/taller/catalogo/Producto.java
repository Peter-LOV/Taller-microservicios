package com.taller.catalogo;

import java.math.BigDecimal;

public record Producto(Long id, String nombre, BigDecimal precio, Integer stock) {}
