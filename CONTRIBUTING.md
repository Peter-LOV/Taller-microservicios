# Guía de contribución

## Flujo de trabajo

1. La rama `main` está protegida: **nadie hace push directo**. Todo entra por Pull Request con al menos 1 aprobación de otro integrante.
2. Cada integrante trabaja en **su propia rama** `feature/<area>` (ver tabla) creada desde `main` actualizada.
3. Se hacen **commits pequeños y con sentido** (uno por tema), no un único commit gigante.
4. Se abre un Pull Request hacia `main` con la plantilla del repositorio.
5. Se fusiona con **"Create a merge commit"** (no *squash*), para conservar la autoría y el historial de cada integrante.
6. Después de fusionar: `git checkout main && git pull origin main` y se borra la rama.

## Convención de commits

Formato: `tipo(ámbito): resumen en minúsculas, en presente, sin punto final` (máx. 72 caracteres), y un cuerpo con viñetas que explique **qué** se hizo y **por qué**.

| Tipo | Cuándo |
|---|---|
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de un error |
| `build` | Dockerfile, Maven, dependencias |
| `test` | Scripts y pruebas |
| `docs` | Documentación y evidencias |
| `chore` | Configuración, archivos de soporte |
| `refactor` | Cambio interno sin cambiar el comportamiento |

Ámbitos usados: `db`, `db-routing`, `catalogo`, `pedidos`, `gateway`, `eureka`, `compose`, `evidencias`, `informe`.

## Responsables

| Parte | Rama | Responsable | Carpetas / archivos |
|---|---|---|---|
| Peter Orrala | `feature/infra-bd-eureka` | <!-- nombre --> | `Dockerfile`, `docker-compose.yml`, `docker/`, `eureka-server/` |
| Integrante B | `feature/persistencia-catalogo` | <!-- nombre --> | `db-routing/`, `servicio-catalogo/` |
| Integrante C | `feature/pedidos-gateway` | <!-- nombre --> | `api-gateway/`, `servicio-pedidos/` |
| Integrante D | `feature/pruebas-docs` | <!-- nombre --> | `README.md`, `docs/`, `requests.http`, `scripts/` |

## Orden de fusión

1. **Base** (líder) → directo a `main`.
2. **A** y **B** en paralelo.
3. **C** después de B (usa el módulo `db-routing`).
4. **D** puede fusionarse en cualquier momento; sus pruebas se ejecutan cuando A, B y C ya están en `main`.
5. **Integración:** `git pull origin main`, `copy .env.example .env`, `docker compose up -d --build`.

## Reglas de oro

- **Nunca** subir `.env`, contraseñas reales ni carpetas `target/`.
- **Nunca** usar `git add .` a ciegas: añade solo los archivos de tu parte.
- **Nunca** `git push --force` sobre `main`.
- Si `main` avanzó mientras trabajabas: `git pull origin main` en tu rama y resuelve los conflictos antes del PR.
