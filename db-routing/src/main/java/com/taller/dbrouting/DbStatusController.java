package com.taller.dbrouting;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expone el estado de los nodos PostgreSQL que ve ESTA instancia (evidencia para las pruebas P05-P08). */
@RestController
public class DbStatusController {

    private final DbRouter router;

    public DbStatusController(DbRouter router) {
        this.router = router;
    }

    @GetMapping("${app.db.base-path:}/db/estado")
    public Map<String, List<Map<String, Object>>> estado() {
        return Map.of("nodos", router.status());
    }
}
