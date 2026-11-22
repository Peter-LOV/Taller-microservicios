package com.taller.catalogo;

import com.taller.catalogo.Solicitudes.CrearProducto;
import com.taller.catalogo.Solicitudes.Reservar;
import com.taller.dbrouting.DbRouter.Result;
import jakarta.validation.Valid;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/catalogo")
public class ProductoController {

    private final ProductoRepository repo;
    private final String instancia;

    public ProductoController(ProductoRepository repo, @Value("${app.instance-id:local}") String instancia) {
        this.repo = repo;
        this.instancia = instancia;
    }

    /** Identifica la instancia que responde (base de las pruebas de balanceo P02 y P03). */
    @GetMapping("/info")
    public Map<String, String> info() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "desconocido";
        }
        return Map.of("servicio", "servicio-catalogo", "instancia", instancia, "host", host);
    }

    @GetMapping("/productos")
    public Respuesta<List<Producto>> listar() {
        Result<List<Producto>> r = repo.listar();
        return new Respuesta<>(instancia, r.nodo(), r.valor());
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<Respuesta<Producto>> buscar(@PathVariable long id,
                                                      @RequestParam(defaultValue = "eventual") String consistencia) {
        Result<java.util.Optional<Producto>> r = repo.buscar(id, "fuerte".equalsIgnoreCase(consistencia));
        if (r.valor().isPresent()) {
            return ResponseEntity.ok(new Respuesta<Producto>(instancia, r.nodo(), r.valor().get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Respuesta<Producto>(instancia, r.nodo(), null));
    }

    @PostMapping("/productos")
    public ResponseEntity<Respuesta<Producto>> crear(@Valid @RequestBody CrearProducto req) {
        Result<Producto> r = repo.crear(req.nombre(), req.precio(), req.stock());
        return ResponseEntity.status(HttpStatus.CREATED).body(new Respuesta<Producto>(instancia, r.nodo(), r.valor()));
    }

    /** Operacion llamada por servicio-pedidos (via Eureka). Descuenta stock y devuelve el precio vigente. */
    @PostMapping("/productos/{id}/reservar")
    public ResponseEntity<?> reservar(@PathVariable long id, @Valid @RequestBody Reservar req) {
        Result<java.util.Optional<Producto>> r = repo.reservar(id, req.cantidad());
        if (r.valor().isPresent()) {
            return ResponseEntity.ok(new Respuesta<Producto>(instancia, r.nodo(), r.valor().get()));
        }
        boolean existe = repo.buscar(id, true).valor().isPresent();
        return ResponseEntity.status(existe ? HttpStatus.CONFLICT : HttpStatus.NOT_FOUND)
                .body(Map.of("error", existe ? "Stock insuficiente" : "Producto no encontrado", "instancia", instancia));
    }
}
