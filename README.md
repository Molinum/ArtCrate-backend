# ArtCrate – Backend (microservicios)

Backend de **ArtCrate**, tienda online de insumos de arte. Proyecto del curso Desarrollo Fullstack 2 (Duoc UC).
Son **6 microservicios Spring Boot**, cada uno con su propia base de datos, que se comunican entre sí con **OpenFeign**.
Es simple a propósito: sin gateway, sin Eureka, sin Docker, sin Lombok y sin seguridad/JWT (todavía).

El front (carpeta `ArtCrate`, al lado de esta) se conectará a estas APIs más adelante. CORS ya está habilitado para `http://localhost:*`.

## Tabla de servicios

| Servicio | Puerto | Qué hace | Llama a | Swagger | Consola H2 (JDBC URL) |
|---|---|---|---|---|---|
| `usuarios-service` | 8081 | Registro, login y CRUD de usuarios | – | [swagger](http://localhost:8081/swagger-ui.html) | [h2](http://localhost:8081/h2-console) `jdbc:h2:file:./data/usuarios-db` |
| `catalogo-service` | 8082 | Categorías y productos | – | [swagger](http://localhost:8082/swagger-ui.html) | [h2](http://localhost:8082/h2-console) `jdbc:h2:file:./data/catalogo-db` |
| `inventario-service` | 8083 | Stock por producto (descontar / reponer) | – | [swagger](http://localhost:8083/swagger-ui.html) | [h2](http://localhost:8083/h2-console) `jdbc:h2:file:./data/inventario-db` |
| `carrito-service` | 8084 | Carrito de compras | catálogo, inventario | [swagger](http://localhost:8084/swagger-ui.html) | [h2](http://localhost:8084/h2-console) `jdbc:h2:file:./data/carrito-db` |
| `pedidos-service` | 8085 | Pedidos y sus estados | carrito, inventario | [swagger](http://localhost:8085/swagger-ui.html) | [h2](http://localhost:8085/h2-console) `jdbc:h2:file:./data/pedidos-db` |
| `pagos-service` | 8086 | Pago simulado de pedidos | pedidos | [swagger](http://localhost:8086/swagger-ui.html) | [h2](http://localhost:8086/h2-console) `jdbc:h2:file:./data/pagos-db` |

Usuario de la consola H2: `sa`, sin contraseña.

```
pagos ──► pedidos ──┬──► carrito ──┬──► catalogo
                    │              └──► inventario
                    └─────────────────► inventario

usuarios: independiente (el front la usa para el login)
```
Las flechas indican quién llama a quién (por Feign, siempre con la URL escrita en `application.properties`).

## Requisitos

- **Java 21** (`java -version`)
- **Maven 3.9+** (`mvn -v`)
- Conexión a internet la primera vez (Maven descarga las dependencias).

Versiones usadas (verificadas entre sí): Spring Boot **3.5.16**, Spring Cloud **2025.0.3** (release train para Boot 3.5; el 2025.1.x es para Boot 4), springdoc **2.8.17** (línea para Boot 3).

## Cómo ejecutar

### Opción 1: todos a la vez (Windows)

Doble clic en **`run-all.bat`** (o ejecútalo desde una terminal). Abre una ventana por servicio.
Espera a que cada ventana muestre `Started ...Application`. Para detener un servicio, cierra su ventana o presiona `Ctrl+C`.

### Opción 2: uno por uno

```bash
cd usuarios-service
mvn spring-boot:run
```
Repite en cada carpeta de servicio. No importa el orden: los servicios solo se llaman entre sí cuando llega una petición.

### Compilar todo

```bash
mvn clean package
```
(en la raíz; el `pom.xml` agregador compila los 6 servicios y deja un `.jar` en la carpeta `target` de cada uno).

Cada servicio crea su base de datos en `./data/<servicio>-db` dentro de su propia carpeta y la llena con **datos de ejemplo la primera vez** (solo si las tablas están vacías). Para volver a empezar de cero, detén los servicios y borra las carpetas `data` (`*/data`).

## Datos de ejemplo

- **Usuarios:** `admin@artcrate.cl` / `Admin123` (rol ADMIN) y `cliente@artcrate.cl` / `Cliente123` (rol CLIENTE, id 2).
- **Catálogo:** 5 categorías (Pintura, Dibujo, Papeles y lienzos, Pinceles y accesorios, Kits ArtCrate) y 12 productos con ids 1 a 12 y precios en CLP.
- **Inventario:** stock para los productos 1 a 12.

## Endpoints

### usuarios-service (8081)

| Método | URL | Descripción |
|---|---|---|
| POST | `/api/usuarios/registro` | Registra un cliente (201). Correo único (409). |
| POST | `/api/usuarios/login` | Valida credenciales y devuelve el usuario sin contraseña (401 si falla). |
| GET | `/api/usuarios` | Lista usuarios |
| GET / PUT / DELETE | `/api/usuarios/{id}` | Obtener, actualizar o eliminar (204) |

### catalogo-service (8082)

| Método | URL | Descripción |
|---|---|---|
| GET / POST | `/api/categorias` | Listar / crear |
| GET / PUT / DELETE | `/api/categorias/{id}` | Obtener / actualizar / eliminar |
| GET | `/api/productos?categoriaId=&q=` | Listar. Filtros opcionales y combinables. |
| POST | `/api/productos` | Crear (201) |
| GET / PUT / DELETE | `/api/productos/{id}` | Obtener / actualizar / eliminar |

### inventario-service (8083)

| Método | URL | Descripción |
|---|---|---|
| GET | `/api/inventario` | Stock de todos los productos |
| GET / PUT | `/api/inventario/{productoId}` | Ver / fijar la cantidad |
| POST | `/api/inventario/descontar` | Descuenta una lista `[{productoId, cantidad}]`: **todo o nada** (409 si falta stock). |
| POST | `/api/inventario/reponer` | Devuelve stock con la misma lista |

### carrito-service (8084)

| Método | URL | Descripción |
|---|---|---|
| GET | `/api/carritos/{usuarioId}` | Ver el carrito (lo crea vacío si no existe), con subtotales y total |
| POST | `/api/carritos/{usuarioId}/items` | Agregar `{productoId, cantidad}` (201) |
| PUT | `/api/carritos/{usuarioId}/items/{productoId}` | Cambiar la cantidad `{cantidad}` |
| DELETE | `/api/carritos/{usuarioId}/items/{productoId}` | Quitar un producto (204) |
| DELETE | `/api/carritos/{usuarioId}` | Vaciar el carrito (204) |

### pedidos-service (8085)

| Método | URL | Descripción |
|---|---|---|
| POST | `/api/pedidos` | Crear pedido `{usuarioId, direccionDespacho, comuna, region}` (201) |
| GET | `/api/pedidos` | Listar todos |
| GET | `/api/pedidos/{id}` | Ver uno |
| GET | `/api/pedidos/usuario/{usuarioId}` | Pedidos de un usuario (el más reciente primero) |
| PATCH | `/api/pedidos/{id}/estado` | Cambiar el estado `{estado}` |

### pagos-service (8086)

| Método | URL | Descripción |
|---|---|---|
| POST | `/api/pagos` | Pagar `{pedidoId, metodo}` con `TARJETA` o `TRANSFERENCIA` (201) |
| GET | `/api/pagos/{id}` | Ver un pago |
| GET | `/api/pagos/pedido/{pedidoId}` | Pagos de un pedido |

## Reglas de negocio

- **Precios** en CLP como números enteros (`long`), nunca decimales.
- **Carrito:** cantidad mínima 1; nunca supera el stock (consulta inventario); si el producto ya está en el carrito, las cantidades se suman; el producto debe existir y estar activo (consulta catálogo, 404 si no); el total es la suma de los subtotales y lo calcula el backend.
- **Stock:** se descuenta al **crear el pedido**, no al agregar al carrito (el carrito no reserva stock).
- **Crear pedido:** lee el carrito (400 si está vacío) → descuenta stock todo o nada (409 si falla) → guarda el pedido `PENDIENTE_PAGO` → vacía el carrito. Si algo falla a mitad de camino, repone el stock y deshace lo guardado.
- **Estados del pedido:** `PENDIENTE_PAGO → PAGADO → ENVIADO → ENTREGADO`. `CANCELADO` solo desde `PENDIENTE_PAGO` y repone el stock. Cualquier otro cambio responde 409.
- **Pagos:** el pedido debe estar `PENDIENTE_PAGO` (409 si no). El pago siempre se aprueba (simulado) y marca el pedido como `PAGADO`.

## Errores

Todos los servicios responden los errores con el mismo formato:

```json
{ "timestamp": "2026-10-07T23:47:09.944", "status": 409, "error": "Conflict", "message": "Stock insuficiente para el producto 3 (disponible: 7, solicitado: 999)" }
```

Códigos: `201` al crear, `204` al borrar, `400` datos inválidos, `401` login incorrecto, `404` no existe, `409` conflicto, `503` otro servicio no disponible.

**Cuando un servicio no puede llamar a otro (Feign):**
- Si el otro servicio no responde (apagado, conexión rechazada o más de 5 segundos): **503** con el mensaje `Servicio inventario no disponible` (o `catalogo`, `carrito`, `pedidos`).
- Si el otro servicio responde 404 o 409, se devuelve el mismo código con su mensaje.

Los **timeouts** (5 s para conectar y 5 s para leer) se configuran en cada `application.properties` con
`spring.cloud.openfeign.client.config.default.connect-timeout` y `...read-timeout`.

## Ejemplos de requests

Con `curl` (en Windows PowerShell escribe `curl.exe`, porque `curl` es otro comando; en Git Bash funciona tal cual):

```bash
# 1) Login
curl -X POST http://localhost:8081/api/usuarios/login -H "Content-Type: application/json" \
  -d '{"correo":"cliente@artcrate.cl","password":"Cliente123"}'

# 2) Ver productos (con filtros)
curl http://localhost:8082/api/productos
curl "http://localhost:8082/api/productos?categoriaId=1&q=acrilico"

# 3) Agregar 2 unidades del producto 1 al carrito del usuario 2
curl -X POST http://localhost:8084/api/carritos/2/items -H "Content-Type: application/json" \
  -d '{"productoId":1,"cantidad":2}'

# 4) Cambiar la cantidad a 3
curl -X PUT http://localhost:8084/api/carritos/2/items/1 -H "Content-Type: application/json" -d '{"cantidad":3}'

# 5) Crear el pedido
curl -X POST http://localhost:8085/api/pedidos -H "Content-Type: application/json" \
  -d '{"usuarioId":2,"direccionDespacho":"Av. Providencia 1234","comuna":"Providencia","region":"Metropolitana de Santiago"}'

# 6) Pagar el pedido 1
curl -X POST http://localhost:8086/api/pagos -H "Content-Type: application/json" -d '{"pedidoId":1,"metodo":"TARJETA"}'

# 7) Cancelar un pedido pendiente (repone el stock)
curl -X PATCH http://localhost:8085/api/pedidos/2/estado -H "Content-Type: application/json" -d '{"estado":"CANCELADO"}'
```

En **Postman**: importa `docs/ArtCrate.postman_collection.json`. La carpeta **"0 - Flujo de compra"** recorre toda la compra en orden (puedes usar *Run collection*). Las URLs y los ids están en variables de la colección.

## Prueba de punta a punta

Se ejecutó este recorrido completo contra los 6 servicios levantados con `run-all.bat`, con **28 de 28 verificaciones correctas**:

1. Login del cliente (y login con clave incorrecta → 401).
2. Listar productos (12, con ids 1 a 12).
3. Agregar 2 productos al carrito y cambiar una cantidad (el total se recalcula).
4. Intentar superar el stock (cambiar cantidad y agregar de más) → 409, y el carrito queda intacto.
5. Crear el pedido → `PENDIENTE_PAGO` con el total correcto.
6. Carrito vacío y stock descontado.
7. Pagar → pago `APROBADO` y pedido `PAGADO`; pagar de nuevo → 409.
8. Crear otro pedido y cancelarlo → el stock vuelve al valor anterior; cancelar uno ya pagado → 409.
9. **Falla:** con `inventario-service` apagado, agregar al carrito y crear un pedido responden **503 "Servicio inventario no disponible"**; carrito y pedidos siguen vivos, el carrito conserva sus ítems y no queda ningún pedido a medias. Al volver el servicio, la compra pendiente se completa.

Pruebas adicionales: descontar stock es todo o nada (3 descuentos simultáneos sobre un stock de 6 → solo uno pasa), tres pagos simultáneos del mismo pedido dejan un solo pago, y si el carrito falla justo al vaciarse, el pedido se deshace y el stock se repone.

## Cambiar a MySQL

En el `application.properties` de cada servicio hay unas líneas comentadas. Para usar MySQL:
1. Agrega la dependencia `com.mysql:mysql-connector-j` al `pom.xml` del servicio.
2. Comenta las líneas de H2 y descomenta las de MySQL (cambia usuario y clave).
3. Crea (o deja que se cree) una base por servicio: `usuarios_db`, `catalogo_db`, etc.

## Decisiones tomadas (cuando algo era ambiguo se eligió lo más simple)

- **Borrado de usuarios y productos:** es físico (la fila se elimina). No se puede borrar una categoría que tenga productos (409).
- **Actualizar usuario** no cambia la contraseña (no estaba pedido). `rol` y `activo` son opcionales en el PUT.
- **Teléfono y región** del usuario no se validan.
- **`GET /api/productos`** devuelve todos los productos, incluidos los inactivos (el campo `activo` permite al front filtrarlos); el carrito sí rechaza los inactivos. La búsqueda `?q=` ignora mayúsculas y tildes y busca en nombre y descripción.
- **`PUT /api/inventario/{productoId}`** crea el registro de stock si el producto aún no tenía uno. `reponer` también.
- **Carrito:** al agregar un producto se copian nombre, precio e imagen del catálogo (si el producto ya estaba, se refrescan). Las operaciones de borrado responden 204 sin cuerpo; las de agregar y cambiar cantidad devuelven el carrito actualizado. No se valida que el `usuarioId` exista en `usuarios-service` (nadie lo pidió, y evita otra dependencia).
- **Pedidos y pagos** se protegen contra operaciones simultáneas con bloqueos de base de datos (inventario y cambio de estado de pedidos), y deshacen a mano lo que ya hicieron si un paso posterior falla.
- **Cliente HTTP de Feign:** el que trae por defecto no soporta el verbo `PATCH`, así que `pagos-service` usa además la dependencia `feign-hc5` (Apache HttpClient 5), que sí lo soporta.
- **Nombre de las propiedades de timeout:** en Spring Cloud OpenFeign 4.3 el prefijo es `spring.cloud.openfeign.client.config.default` (el antiguo `feign.client.config` ya no existe).
- Los errores `405` (método no permitido) de rutas inexistentes usan el formato por defecto de Spring Boot.
- No hay pruebas unitarias automáticas (solo las pruebas manuales de arriba), ni seguridad, JWT ni validación de roles.
- **Carpetas `data`:** las bases H2 en archivo se crean relativas a la carpeta desde la que se ejecuta cada servicio (con `run-all.bat`, dentro de cada servicio). Están en el `.gitignore`.

## Estructura de cada servicio

```
<servicio>/
├── pom.xml
└── src/main/
    ├── java/cl/artcrate/<servicio>/
    │   ├── controller/   # endpoints REST (reciben la petición y llaman al servicio)
    │   ├── service/      # lógica de negocio
    │   ├── repository/   # acceso a la base de datos (Spring Data JPA)
    │   ├── model/        # entidades JPA y enums
    │   ├── dto/          # records de entrada y salida (nunca se exponen las entidades)
    │   ├── client/       # interfaces Feign (solo carrito, pedidos y pagos)
    │   ├── config/       # CORS y datos iniciales (seed)
    │   └── exception/    # excepciones y manejador de errores uniforme
    └── resources/application.properties
```
