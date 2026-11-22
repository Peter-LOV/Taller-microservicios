package com.taller.pedidos;

import com.taller.pedidos.Errores.CatalogoNoDisponible;
import com.taller.pedidos.Errores.ProductoNoEncontrado;
import com.taller.pedidos.Errores.StockInsuficiente;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/** Cliente de servicio-catalogo. Usa SOLO su API REST: nunca toca las tablas del catalogo. */
@Component
public class CatalogoClient {

    public record Producto(Long id, String nombre, BigDecimal precio, Integer stock) {}

    public record RespuestaCatalogo(String instancia, String nodoBd, Producto datos) {}

    private record ReservaRequest(int cantidad) {}

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);
    private static final String BASE = "http://servicio-catalogo/catalogo"; // nombre logico, no host:puerto
    private static final int MAX_INTENTOS = 3;

    private final RestTemplate rest;

    public CatalogoClient(RestTemplate rest) {
        this.rest = rest;
    }

    /**
     * Reserva stock y obtiene el precio. Solo se reintenta cuando la conexion ni siquiera se establecio
     * (la peticion no llego al servidor), porque reservar NO es idempotente: reintentar tras un timeout
     * de lectura podria descontar el stock dos veces.
     */
    public RespuestaCatalogo reservar(long productoId, int cantidad) {
        for (int intento = 1; ; intento++) {
            try {
                RespuestaCatalogo r = rest.postForObject(BASE + "/productos/{id}/reservar",
                        new ReservaRequest(cantidad), RespuestaCatalogo.class, productoId);
                log.info("[CATALOGO] reserva OK producto={} atendida por instancia={} (intento {})",
                        productoId, r.instancia(), intento);
                return r;
            } catch (HttpClientErrorException.NotFound e) {
                throw new ProductoNoEncontrado(productoId);
            } catch (HttpClientErrorException.Conflict e) {
                throw new StockInsuficiente(productoId);
            } catch (HttpServerErrorException e) {
                throw new CatalogoNoDisponible(e);
            } catch (ResourceAccessException e) {
                Throwable c = e.getCause();
                boolean noLlego = c instanceof ConnectException || c instanceof UnknownHostException
                        || c instanceof NoRouteToHostException;
                log.warn("[CATALOGO] fallo de conexion (intento {}/{}): {}", intento, MAX_INTENTOS, e.getMessage());
                if (!noLlego || intento >= MAX_INTENTOS) {
                    throw new CatalogoNoDisponible(e);
                }
            } catch (IllegalStateException e) { // LoadBalancer: no hay instancias registradas en Eureka
                throw new CatalogoNoDisponible(e);
            }
        }
    }
}
