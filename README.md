# Microservicios con descubrimiento dinámico, balanceo de carga y persistencia replicada

Sistema distribuido con **Spring Boot**, **Eureka**, **Spring Cloud Gateway** y **PostgreSQL replicado**
(1 primario + 2 réplicas), desplegable con Docker Compose.

> El README técnico completo (arquitectura, ejecución, operación y pruebas) llegará con la parte D.

## Equipo

| Integrante | Rol | Carpeta / archivos |
|---|---|---|
| Peter Orrala | A · Infraestructura, PostgreSQL y Eureka | `docker/`, `Dockerfile`, `docker-compose.yml`, `eureka-server/` |
| <!-- completar nombre --> | B · Persistencia y catálogo | `db-routing/`, `servicio-catalogo/` |
| <!-- completar nombre --> | C · Pedidos y Gateway | `servicio-pedidos/`, `api-gateway/` |
| <!-- completar nombre --> | D · Pruebas y documentación | `scripts/`, `docs/`, `requests.http`, `README.md` |

## Estado

- [x] Estructura base
- [x] A · Infraestructura y Eureka
- [ ] B · db-routing y catálogo
- [ ] C · Pedidos y Gateway
- [ ] D · Pruebas y documentación
