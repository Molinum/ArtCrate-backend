# Arquitectura del backend

ArtCrate es una tienda online de insumos de arte. El backend está hecho con microservicios Spring Boot: cada servicio se encarga de una parte del negocio y tiene su propia base de datos MySQL. Un servidor Eureka registra los servicios y un API Gateway recibe todas las peticiones del front.

## Componentes

| Componente | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| gateway-service | 8080 | Punto de entrada único, enruta y aplica CORS | no usa |
| eureka-server | 8761 | Registro de servicios | no usa |
| usuarios-service | 8081 | Registro, login y CRUD de usuarios | usuarios_db |
| catalogo-service | 8082 | Categorías y productos | catalogo_db |
| inventario-service | 8083 | Stock por producto | inventario_db |
| carrito-service | 8084 | Carrito de compras | carrito_db |
| pedidos-service | 8085 | Pedidos y sus estados | pedidos_db |
| pagos-service | 8086 | Pago simulado de pedidos | pagos_db |
| MySQL (Docker) | 3306 | Base de datos de los 6 servicios | |

## Diagrama

```
          front / Postman
                 |
                 v
        +------------------+        +----------------+
        |  gateway  :8080  |<------>| Eureka  :8761  |
        +--------+---------+        +-------^--------+
                 |                          | (todos se registran)
  +--------+-----+------+---------+---------+---------+
  |        |            |         |         |         |
usuarios catalogo   inventario  carrito  pedidos    pagos
 :8081    :8082       :8083      :8084    :8085     :8086
  |        |            |         |         |         |
  v        v            v         v         v         v
usuarios catalogo   inventario  carrito  pedidos    pagos
  _db      _db          _db       _db       _db       _db
  \________________________  MySQL :3306  ______________/
```

## Comunicación

**Front a backend.** El front solo llama al gateway (`http://localhost:8080`). El gateway mira el inicio de la ruta y la manda al servicio que corresponde:

| Ruta | Servicio |
|---|---|
| `/api/usuarios/**` | usuarios-service |
| `/api/categorias/**`, `/api/productos/**` | catalogo-service |
| `/api/inventario/**` | inventario-service |
| `/api/carritos/**` | carrito-service |
| `/api/pedidos/**` | pedidos-service |
| `/api/pagos/**` | pagos-service |

CORS se configura solo en el gateway (`http://localhost:*`).

**Swagger.** Cada servicio expone su documentación OpenAPI. El gateway lee el JSON de los 6 (rutas `/docs/<servicio>/v3/api-docs`) y los muestra en una sola pantalla en `http://localhost:8080/swagger-ui.html`. Las rutas están en `gateway-service/src/main/resources/application.yml`.

**Descubrimiento de servicios.** Cada servicio se registra en Eureka con su `spring.application.name`. El gateway y los clientes Feign usan ese nombre en vez de una URL fija (`lb://usuarios-service`, `@FeignClient(name = "inventario-service")`).

**Entre servicios.** Las llamadas internas van directo con OpenFeign, sin pasar por el gateway:

| Servicio | Llama a | Para |
|---|---|---|
| carrito | catalogo | validar el producto y copiar nombre, precio e imagen |
| carrito | inventario | consultar el stock disponible |
| pedidos | carrito | leer el carrito y vaciarlo al crear el pedido |
| pedidos | inventario | descontar o reponer stock |
| pagos | pedidos | leer el pedido y marcarlo como pagado |

Si un servicio no responde, el que lo llamó responde 503 con un mensaje (por ejemplo "Servicio inventario no disponible").

## Base de datos

Hay una sola instancia de MySQL 8.4 en un contenedor Docker (`docker-compose.yml`), con una base de datos por servicio. Cada servicio solo accede a la suya.

| Base | Tablas |
|---|---|
| usuarios_db | usuarios |
| catalogo_db | categorias, productos |
| inventario_db | stock |
| carrito_db | carritos, items_carrito |
| pedidos_db | pedidos, items_pedido |
| pagos_db | pagos |

- Las bases se crean con `docker/mysql-init/01-crear-bases.sql` la primera vez que se levanta el contenedor.
- Las tablas las crea Hibernate al arrancar cada servicio (`spring.jpa.hibernate.ddl-auto=update`) y se cargan datos de ejemplo si están vacías.
- Entre servicios no hay llaves foráneas: se referencian solo por id (`usuarioId`, `productoId`, `pedidoId`), y la validez se revisa llamando al otro servicio.
- La conexión se configura con las variables `DB_HOST`, `DB_PORT`, `DB_USER` y `DB_PASSWORD`. Por defecto: `localhost`, `3306`, `artcrate`, `artcrate`.
- Los datos se guardan en un volumen de Docker. `docker compose down -v` los borra.

## Flujo de una compra

1. El cliente agrega productos al carrito (`carrito-service`). El carrito consulta al catálogo y al inventario.
2. Se crea el pedido (`pedidos-service`): lee el carrito, descuenta el stock en inventario, guarda el pedido como `PENDIENTE_PAGO` y vacía el carrito. Si un paso falla, repone el stock y deshace lo guardado.
3. Se paga el pedido (`pagos-service`): comprueba que esté `PENDIENTE_PAGO`, registra el pago aprobado y cambia el pedido a `PAGADO`.

Estados del pedido: `PENDIENTE_PAGO`, `PAGADO`, `ENVIADO`, `ENTREGADO` y `CANCELADO` (solo desde `PENDIENTE_PAGO`, y repone el stock).

## Orden de arranque

1. MySQL (`docker compose up -d`).
2. Eureka.
3. Los 6 microservicios. Fallan al arrancar si MySQL no está disponible.
4. El gateway.

`run-all.bat` y `run-all.sh` arrancan Eureka, los servicios y el gateway en ese orden, pero no levantan MySQL.

## Estructura de un servicio

```
<servicio>/src/main/java/cl/artcrate/<servicio>/
├── controller/   endpoints REST
├── service/      lógica de negocio
├── repository/   acceso a datos (Spring Data JPA)
├── model/        entidades y enums
├── dto/          objetos de entrada y salida
├── client/       clientes Feign (carrito, pedidos y pagos)
├── config/       datos iniciales
└── exception/    manejo de errores
```

## Limitaciones

- No hay seguridad: sin JWT ni validación de roles.
- No hay pruebas automáticas, solo pruebas manuales con Postman (`docs/ArtCrate.postman_collection.json`).
- Todas las bases comparten una instancia de MySQL. Para un entorno real convendría una instancia por servicio.
