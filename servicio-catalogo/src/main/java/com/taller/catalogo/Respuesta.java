package com.taller.catalogo;

/** Envoltorio de respuesta: indica QUE instancia respondio y QUE nodo de base de datos se uso. */
public record Respuesta<T>(String instancia, String nodoBd, T datos) {}
