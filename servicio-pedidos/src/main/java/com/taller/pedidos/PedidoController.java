package com.taller.pedidos;

import com.taller.dbrouting.DbRouter.Result;
import com.taller.pedidos.CatalogoClient.RespuestaCatalogo;
import com.taller.pedidos.Solicitudes.CrearPedido;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    public record RespuestaPedido(String instancia, String nodoBd, String catalogoInstancia, Pedido pedido) {}

    public record RespuestaLista(String instancia, String nodoBd, List<Pedido> datos) {}

    public record RespuestaUno(String instancia, String nodoBd, Pedido datos) {}

    private final PedidoRepository repo;
    private final CatalogoClient catalogo;
    private final String instancia;

    public PedidoController(PedidoRepository repo, CatalogoClient catalogo,
                            @Value("${app.instance-id:local}") String instancia) {
        this.repo = repo;
        this.catalogo = catalogo;
        this.instancia = instancia;
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "desconocido";
        }
        return Map.of("servicio", "servicio-pedidos", "instancia", instancia, "host", host);
    }

    /** Operacion de negocio con llamada real entre microservicios: pedidos -> (Eureka) -> catalogo. */
    @PostMapping
    public ResponseEntity<RespuestaPedido> crear(@Valid @RequestBody CrearPedido req) {
        RespuestaCatalogo reserva = catalogo.reservar(req.productoId(), req.cantidad());
        BigDecimal precio = reserva.datos().precio();
        BigDecimal total = precio.multiply(BigDecimal.valueOf(req.cantidad()));
        Result<Pedido> r = repo.crear(req.productoId(), req.cantidad(), precio, total);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RespuestaPedido(instancia, r.nodo(), reserva.instancia(), r.valor()));
    }

    @GetMapping
    public RespuestaLista listar() {
        Result<List<Pedido>> r = repo.listar();
        return new RespuestaLista(instancia, r.nodo(), r.valor());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaUno> buscar(@PathVariable long id,
                                               @RequestParam(defaultValue = "eventual") String consistencia) {
        Result<Optional<Pedido>> r = repo.buscar(id, "fuerte".equalsIgnoreCase(consistencia));
        if (r.valor().isPresent()) {
            return ResponseEntity.ok(new RespuestaUno(instancia, r.nodo(), r.valor().get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new RespuestaUno(instancia, r.nodo(), null));
    }
}
