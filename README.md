# ArtCrate – Backend (microservicios)

Backend de **ArtCrate**, tienda online de insumos de arte. Proyecto del curso Desarrollo Fullstack 2 (Duoc UC).
Son **6 microservicios Spring Boot**, cada uno con su propia base de datos, más un servidor **Eureka** (registro de servicios) y un **API Gateway** (puerta de entrada única).
Los servicios se encuentran por su nombre gracias a Eureka y se llaman entre sí con **OpenFeign**.
Es simple a propósito: sin Docker, sin Lombok y sin seguridad/JWT (todavía).

**El front (carpeta `ArtCrate`, al lado de esta) debe llamar SOLO a `http://localhost:8080` (el gateway).** El gateway es el único lugar con CORS (`http://localhost:*`).
Los servicios también se pueden llamar directo por su puerto (para Swagger y pruebas), pero sin CORS.

**Swagger a través del gateway:** además del Swagger de cada servicio (tabla de abajo), el gateway junta los 6 en una sola pantalla:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html), con un selector arriba a la derecha para elegir el servicio.
El gateway solo lee el JSON de OpenAPI de cada servicio (rutas `/docs/<servicio>/v3/api-docs` en `gateway-service/src/main/resources/application.yml`, reenviadas a `/v3/api-docs` de cada uno); la pantalla de Swagger existe una sola vez, en el propio gateway.
Cada servicio declara dos servidores posibles para "Probar" (*Try it out*), en su `config/OpenApiConfig.java`: el gateway (`http://localhost:8080`, primero y recomendado) y su propio puerto (para probarlo aislado, sin pasar por el gateway). Tiene que ser una lista fija: como el gateway es el único lugar con CORS habilitado, si "Probar" llamara directo al puerto del servicio desde la pantalla agregada del gateway, el navegador lo bloquearía.

## Tabla de servicios

| Servicio | Puerto | Qué hace | Llama a | Swagger | Consola H2 (JDBC URL) |
|---|---|---|---|---|---|
| `gateway-service` | **8080** | **API Gateway**: única puerta de entrada, reenvía cada ruta al servicio correcto | Eureka (para encontrar los servicios) | – | – |
| `eureka-server` | **8761** | **Registro de servicios**: panel en [localhost:8761](http://localhost:8761) | – | – | – |
| `usuarios-service` | 8081 | Registro, login y CRUD de usuarios | – | [swagger](http://localhost:8081/swagger-ui.html) | [h2](http://localhost:8081/h2-console) `jdbc:h2:file:./data/usuarios-db` |
| `catalogo-service` | 8082 | Categorías y productos | – | [swagger](http://localhost:8082/swagger-ui.html) | [h2](http://localhost:8082/h2-console) `jdbc:h2:file:./data/catalogo-db` |
| `inventario-service` | 8083 | Stock por producto (descontar / reponer) | – | [swagger](http://localhost:8083/swagger-ui.html) | [h2](http://localhost:8083/h2-console) `jdbc:h2:file:./data/inventario-db` |
| `carrito-service` | 8084 | Carrito de compras | catálogo, inventario | [swagger](http://localhost:8084/swagger-ui.html) | [h2](http://localhost:8084/h2-console) `jdbc:h2:file:./data/carrito-db` |
| `pedidos-service` | 8085 | Pedidos y sus estados | carrito, inventario | [swagger](http://localhost:8085/swagger-ui.html) | [h2](http://localhost:8085/h2-console) `jdbc:h2:file:./data/pedidos-db` |
| `pagos-service` | 8086 | Pago simulado de pedidos | pedidos | [swagger](http://localhost:8086/swagger-ui.html) | [h2](http://localhost:8086/h2-console) `jdbc:h2:file:./data/pagos-db` |

Usuario de la consola H2: `sa`, sin contraseña.

```
      front / Postman
            │  (todo pasa por aquí)
            ▼
   ┌─────────────────┐   consulta dónde está cada servicio    ┌───────────────┐
   │  gateway  :8080 │◄──────────────────────────────────────►│ Eureka  :8761 │
   └────────┬────────┘                                        └───────▲───────┘
            │ reenvía según la ruta (lb://nombre)                     │ cada servicio se registra
   ┌────────┼───────────┬────────────┬────────────┬──────────┐        │ al arrancar
   ▼        ▼           ▼            ▼            ▼          ▼        │
usuarios  catalogo  inventario    carrito      pedidos     pagos ────┘
 :8081     :8082      :8083        :8084        :8085      :8086

Entre servicios (Feign, por nombre, sin pasar por el gateway):
pagos ──► pedidos ──┬──► carrito ──┬──► catalogo
                    │              └──► inventario
                    └─────────────────► inventario
```

**Cómo se encuentran los servicios:** al arrancar, cada servicio se registra en Eureka con su `spring.application.name`
(`usuarios-service`, `catalogo-service`...) y le avisa cada 5 s que sigue vivo. Los clientes Feign no llevan URL, solo el nombre
del servicio destino (`@FeignClient(name = "inventario-service")`): Feign le pregunta a Eureka dónde está y el balanceador de carga hace la llamada.

**Cómo enruta el gateway:** mira el inicio de la ruta y la envía al servicio que corresponde (`lb://` = buscarlo por nombre en Eureka):

| Si la ruta empieza con... | va a... |
|---|---|
| `/api/usuarios/**` | `usuarios-service` |
| `/api/categorias/**` y `/api/productos/**` | `catalogo-service` |
| `/api/inventario/**` | `inventario-service` |
| `/api/carritos/**` | `carrito-service` |
| `/api/pedidos/**` | `pedidos-service` |
| `/api/pagos/**` | `pagos-service` |

Las rutas están en `gateway-service/src/main/resources/application.yml`.
Las flechas indican quién llama a quién (por Feign, siempre con la URL escrita en `application.properties`).

## Requisitos

- **Java 21** (`java -version`)
- **Maven 3.9+** (`mvn -v`)
- Conexión a internet la primera vez (Maven descarga las dependencias).

Versiones usadas (verificadas entre sí): Spring Boot **3.5.16**, Spring Cloud **2025.0.3** (release train para Boot 3.5; el 2025.1.x es para Boot 4; incluye Eureka/Netflix 4.3.3, OpenFeign 4.3.3 y Gateway 4.3.5), springdoc **2.8.17** (línea para Boot 3).

## Cómo ejecutar

### Opción 1: todos a la vez (Windows)

Doble clic en **`run-all.bat`** (o ejecútalo desde una terminal). Abre una ventana por servicio, en este **orden de arranque**:

1. **Eureka** (8761) y espera 20 segundos a que quede listo.
2. Los **6 microservicios** (8081 a 8086), que se registran solos en Eureka.
3. El **gateway** (8080), al final.

Espera a que cada ventana muestre `Started ...Application`. Para detener un servicio, cierra su ventana o presiona `Ctrl+C`.
Para comprobar que todo está registrado, abre el **panel de Eureka**: http://localhost:8761 (debe listar los 7 servicios en estado `UP`).
Un servicio nuevo tarda unos segundos en aparecer y en ser visto por los demás.

### En Linux (Arch), macOS o Git Bash: `run-all.sh`

```bash
sudo pacman -S jdk21-openjdk maven     # solo en Arch, si aún no los tienes
archlinux-java status                  # comprueba que Java 21 sea el activo (si no: sudo archlinux-java set java-21-openjdk)

chmod +x run-all.sh stop-all.sh        # solo si el repo se copió sin permisos de ejecución
./run-all.sh                           # arranca todo, con pausas entre cada servicio
./stop-all.sh                          # detiene todo
```

`run-all.sh` sigue el mismo orden que el `.bat` (Eureka → 6 microservicios → gateway) pero **en segundo plano** y con el mismo terminal libre:
- Compila los 8 módulos si falta algún `.jar` (o siempre, con `./run-all.sh --build`) y arranca cada servicio con `java -jar`.
- Espera a que Eureka abra su puerto y 10 s más; luego deja **3 s entre cada servicio** y **5 s antes del gateway**. Al final espera a que todos abran su puerto y muestra cuántos quedaron registrados en Eureka (deben ser 7 de 7).
- La salida de cada servicio va a `logs/<servicio>.log` (por ejemplo `tail -f logs/carrito-service.log`) y su PID a `.pids/`. Si un servicio muere al arrancar, el script lo avisa y muestra las últimas líneas de su log.
- Si un puerto ya está en uso, ese servicio se omite (así se puede volver a ejecutar sin duplicar procesos).
- Cambia las pausas o la memoria con variables de entorno, por ejemplo en un portátil con poca RAM:
  `ESPERA_ENTRE=5 JAVA_OPTS="-Xmx256m" ./run-all.sh` (también existen `ESPERA_TRAS_EUREKA`, `ESPERA_ANTES_GATEWAY` y `MAX_ESPERA_PUERTO`).
- `./stop-all.sh` cierra cada servicio de forma normal (el gateway primero y Eureka al final) y fuerza el cierre si tarda más de 15 s.

No hace falta tocar `/etc/hosts` ni la red: todos los servicios se registran en Eureka como `localhost`.

Los scripts se probaron con el bash de Git Bash en Windows (arranque, omisión de servicios ya corriendo, detención, y los errores de falta de Java y de JVM que muere al arrancar). **No se pudieron probar en Arch Linux real**; solo usan bash estándar, `java` y (opcionalmente) `curl`.

### Opción 2: uno por uno

```bash
cd eureka-server
mvn spring-boot:run        # primero Eureka (8761)
```
Luego, en otra terminal por cada carpeta: `usuarios-service`, `catalogo-service`, `inventario-service`, `carrito-service`, `pedidos-service`, `pagos-service`
(cada uno con `mvn spring-boot:run`) y al final `gateway-service`. Si arrancas un servicio antes que Eureka, reintenta registrarse solo.

### Compilar todo

```bash
mvn clean package
```
(en la raíz; el `pom.xml` agregador compila los 8 módulos y deja un `.jar` en la carpeta `target` de cada uno).

Cada servicio crea su base de datos en `./data/<servicio>-db` dentro de su propia carpeta y la llena con **datos de ejemplo la primera vez** (solo si las tablas están vacías). Para volver a empezar de cero, detén los servicios y borra las carpetas `data` (`*/data`).

## Datos de ejemplo

- **Usuarios:** `admin@artcrate.cl` / `Admin123` (rol ADMIN) y `cliente@artcrate.cl` / `Cliente123` (rol CLIENTE, id 2).
- **Catálogo:** 5 categorías (Pintura, Dibujo, Papeles y lienzos, Pinceles y accesorios, Kits ArtCrate) y 12 productos con ids 1 a 12 y precios en CLP.
- **Inventario:** stock para los productos 1 a 12.

## Endpoints

Todas las URLs de esta sección se llaman por el **gateway**: `http://localhost:8080` + la ruta (por ejemplo `http://localhost:8080/api/productos`).
El número de puerto de cada título es el del servicio, por si lo quieres llamar directo.

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

**Cuando un servicio no puede llamar a otro (Feign, por nombre con Eureka):**
- Si el otro servicio no responde (apagado, conexión rechazada, más de 5 segundos, o ya no está registrado en Eureka): **503** con el mensaje `Servicio inventario no disponible` (o `catalogo`, `carrito`, `pedidos`).
- Si el otro servicio responde 404 o 409, se devuelve el mismo código con su mensaje.

**Errores del propio gateway:** el gateway también responde con el mismo JSON uniforme (`ManejadorErroresGateway`):
- Si el servicio de la ruta está caído, no tiene instancias en Eureka o no responde a tiempo: **503** `Servicio inventario no disponible` (el nombre es el de la ruta: `usuarios`, `catalogo`, `inventario`, `carrito`, `pedidos` o `pagos`). Es igual si el servicio acaba de caer o si Eureka ya lo eliminó.
- Una ruta que no existe (`/api/nada`): **404** `No existe la ruta /api/nada`.
- Cualquier otro fallo inesperado del gateway: **500** `Error interno del servidor`.

Los errores de negocio de los servicios (400, 404, 409, 503 con mensaje, etc.) pasan por el gateway **sin modificarse**.

Los **timeouts** (5 s para conectar y 5 s para leer) se configuran en cada `application.properties` con
`spring.cloud.openfeign.client.config.default.connect-timeout` y `...read-timeout`.

## Ejemplos de requests

Con `curl` (en Windows PowerShell escribe `curl.exe`, porque `curl` es otro comando; en Git Bash funciona tal cual):

```bash
# 1) Login
curl -X POST http://localhost:8080/api/usuarios/login -H "Content-Type: application/json" \
  -d '{"correo":"cliente@artcrate.cl","password":"Cliente123"}'

# 2) Ver productos (con filtros)
curl http://localhost:8080/api/productos
curl "http://localhost:8080/api/productos?categoriaId=1&q=acrilico"

# 3) Agregar 2 unidades del producto 1 al carrito del usuario 2
curl -X POST http://localhost:8080/api/carritos/2/items -H "Content-Type: application/json" \
  -d '{"productoId":1,"cantidad":2}'

# 4) Cambiar la cantidad a 3
curl -X PUT http://localhost:8080/api/carritos/2/items/1 -H "Content-Type: application/json" -d '{"cantidad":3}'

# 5) Crear el pedido
curl -X POST http://localhost:8080/api/pedidos -H "Content-Type: application/json" \
  -d '{"usuarioId":2,"direccionDespacho":"Av. Providencia 1234","comuna":"Providencia","region":"Metropolitana de Santiago"}'

# 6) Pagar el pedido 1
curl -X POST http://localhost:8080/api/pagos -H "Content-Type: application/json" -d '{"pedidoId":1,"metodo":"TARJETA"}'

# 7) Cancelar un pedido pendiente (repone el stock)
curl -X PATCH http://localhost:8080/api/pedidos/2/estado -H "Content-Type: application/json" -d '{"estado":"CANCELADO"}'
```

En **Postman**: importa `docs/ArtCrate.postman_collection.json`. La carpeta **"0 - Flujo de compra"** recorre toda la compra en orden (puedes usar *Run collection*). Todas las peticiones usan la variable `{{url}}` = `http://localhost:8080` (el gateway); los ids también son variables de la colección.

## Pruebas realizadas

**Eureka:** con Eureka y los 6 servicios arriba, el panel de Eureka lista los 6 en estado `UP` (7 con el gateway) y el flujo completo de abajo funciona llamando directo a cada puerto.

**De punta a punta, usando SOLO el puerto 8080** (gateway), con todo levantado por `run-all.bat`: **28 de 28 verificaciones correctas**:

1. Login del cliente (y login con clave incorrecta → 401).
2. Listar productos (12, con ids 1 a 12).
3. Agregar 2 productos al carrito y cambiar una cantidad (el total se recalcula).
4. Intentar superar el stock (cambiar cantidad y agregar de más) → 409, y el carrito queda intacto.
5. Crear el pedido → `PENDIENTE_PAGO` con el total correcto.
6. Carrito vacío y stock descontado.
7. Pagar → pago `APROBADO` y pedido `PAGADO`; pagar de nuevo → 409.
8. Crear otro pedido y cancelarlo → el stock vuelve al valor anterior; cancelar uno ya pagado → 409.
9. **Falla:** con `inventario-service` apagado, agregar al carrito y crear un pedido responden **503 "Servicio inventario no disponible"**; carrito y pedidos siguen vivos, el carrito conserva sus ítems y no queda ningún pedido a medias. **El gateway sigue funcionando** (mismo proceso, las demás rutas responden). Al volver el servicio, la compra pendiente se completa (en unos segundos, lo que tarda Eureka en volver a verlo).

**Errores del gateway:** con `inventario-service` apagado, `GET /api/inventario` por el gateway responde 503 con el JSON uniforme (tanto en los segundos en que Eureka aún lo lista como cuando ya lo eliminó); lo mismo con `catalogo-service` (mensaje "Servicio catalogo no disponible"); una ruta inexistente responde 404; los errores de los servicios (400, 404...) pasan sin cambios; las demás rutas siguen funcionando.

**Gateway y CORS:** una petición con `Origin: http://localhost:5173` recibe una sola cabecera `Access-Control-Allow-Origin`; un origen no permitido recibe 403; los servicios llamados directo ya no envían cabeceras CORS.

**Pruebas adicionales:** descontar stock es todo o nada (3 descuentos simultáneos sobre un stock de 6 → solo uno pasa), tres pagos simultáneos del mismo pedido dejan un solo pago, y si el carrito falla justo al vaciarse, el pedido se deshace y el stock se repone.

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
- **Eureka en modo desarrollo:** para que todo reaccione rápido se acortaron los tiempos por defecto: el servicio se registra 5 s después de arrancar (antes 40 s), avisa que sigue vivo cada 5 s y se elimina tras 15 s sin avisar; los clientes actualizan la lista cada 5 s y el balanceador de carga recuerda las direcciones 5 s (antes 35 s). En el servidor se desactivó la "autopreservación" (si no, con pocos servicios Eureka dejaría listados los que están caídos). Estos valores están comentados en cada `application.properties` / `application.yml`; en producción conviene volver a los valores por defecto.
- **Eureka registra todo como `localhost`** (`eureka.instance.hostname=localhost` en los 6 servicios y el gateway). Por defecto Eureka registra el nombre del equipo (hostname), y si ese nombre no resuelve a una IP (por ejemplo, en una instalación nueva de Arch sin entrada en `/etc/hosts`) las llamadas entre servicios fallan con 503. Como los 8 procesos corren en la misma máquina, `localhost` siempre funciona, sin depender del nombre del equipo ni de la red. Si algún día los servicios corren en máquinas distintas, hay que quitar esta propiedad.
- **Gateway:** en el release train 2025.0 el starter se llama `spring-cloud-starter-gateway-server-webflux` (el antiguo `spring-cloud-starter-gateway` está obsoleto) y las propiedades van bajo `spring.cloud.gateway.server.webflux.*`. Es reactivo, así que su módulo no incluye `spring-boot-starter-web`. Las rutas son explícitas (no se usa el descubrimiento automático de rutas).
- **CORS** solo en el gateway; los 6 servicios no lo configuran. Si el front llamara directo a un servicio, el navegador lo bloquearía: hay que usar el puerto 8080.
- **`run-all.bat`** espera con `timeout`; si no hay consola interactiva (por ejemplo, al lanzarlo desde otro programa) usa `ping` como respaldo, porque `timeout` falla en ese caso.
- **Carpetas `data`:** las bases H2 en archivo se crean relativas a la carpeta desde la que se ejecuta cada servicio (con `run-all.bat`, dentro de cada servicio). Están en el `.gitignore`.

## Estructura del repositorio

```
artcrate-backend/
├── pom.xml              # agregador de los 8 módulos
├── run-all.bat          # arranca todo en orden (Windows)
├── run-all.sh           # arranca todo en orden, en segundo plano (Linux / macOS / Git Bash)
├── stop-all.sh          # detiene lo que lanzó run-all.sh
├── docs/                # colección Postman
├── eureka-server/       # registro de servicios (8761)
├── gateway-service/     # API Gateway (8080); rutas y CORS en application.yml
└── <6 microservicios>/  # usuarios, catalogo, inventario, carrito, pedidos, pagos
```

## Estructura de cada microservicio

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
