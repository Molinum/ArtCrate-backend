# ArtCrate Backend

Backend de ArtCrate, una tienda online de insumos de arte. Proyecto del curso Desarrollo Fullstack 2 (Duoc UC).

Está formado por 6 microservicios Spring Boot, un servidor Eureka para el registro de servicios y un API Gateway como único punto de entrada. Cada microservicio tiene su propia base de datos MySQL.

Más detalle del diseño en [docs/arquitectura.md](docs/arquitectura.md).

## Tecnologías

- Java 21
- Spring Boot 3.5.16 (Web, Data JPA, Validation)
- Spring Cloud 2025.0.3 (Eureka, OpenFeign, Gateway)
- MySQL 8.4 con Docker
- springdoc-openapi 2.8.17 (Swagger)
- Maven

## Servicios

| Servicio | Puerto | Descripción | Base de datos |
|---|---|---|---|
| gateway-service | 8080 | API Gateway, punto de entrada del front | - |
| eureka-server | 8761 | Registro de servicios | - |
| usuarios-service | 8081 | Registro, login y CRUD de usuarios | usuarios_db |
| catalogo-service | 8082 | Categorías y productos | catalogo_db |
| inventario-service | 8083 | Stock por producto | inventario_db |
| carrito-service | 8084 | Carrito de compras | carrito_db |
| pedidos-service | 8085 | Pedidos y sus estados | pedidos_db |
| pagos-service | 8086 | Pago simulado de pedidos | pagos_db |

El front debe llamar solo al gateway (`http://localhost:8080`), que es el único con CORS configurado. Cada servicio también tiene Swagger en `http://localhost:<puerto>/swagger-ui.html`. Además, el gateway junta los 6 en una sola pantalla: `http://localhost:8080/swagger-ui.html`, con un selector arriba a la derecha para elegir el servicio. Desde ahí, "Try it out" llama por el gateway (cada servicio lo declara en su `config/OpenApiConfig.java`), porque es el único con CORS habilitado.

## Requisitos

- Java 21
- Maven 3.9 o superior
- Docker con `docker compose`

## Cómo ejecutar

### 1. Base de datos

```bash
docker compose up -d
docker compose ps        # esperar a que aparezca "healthy"
```

Esto levanta MySQL en el puerto 3306 y crea las bases `usuarios_db`, `catalogo_db`, `inventario_db`, `carrito_db`, `pedidos_db` y `pagos_db`. Las tablas las crea cada servicio al arrancar.

El usuario de la base es `artcrate` con clave `artcrate` (solo para desarrollo). Se puede cambiar con las variables de entorno `DB_HOST`, `DB_PORT`, `DB_USER` y `DB_PASSWORD`.

Para borrar los datos y empezar de cero: `docker compose down -v`.

### 2. Servicios

Con MySQL arriba, compilar y levantar todo:

```bash
mvn clean package
```

- Windows: `run-all.bat`
- Linux, macOS o Git Bash: `./run-all.sh` (se detiene con `./stop-all.sh`)

Los scripts arrancan Eureka, luego los 6 servicios y al final el gateway. El panel de Eureka (`http://localhost:8761`) debe mostrar los 7 servicios en estado `UP`.

`run-all.sh` guarda los logs en `logs/<servicio>.log`. Las pausas y la memoria se pueden ajustar con variables de entorno, por ejemplo `ESPERA_ENTRE=5 JAVA_OPTS="-Xmx256m" ./run-all.sh`.

Para levantar un servicio por separado, entrar a su carpeta y ejecutar `mvn spring-boot:run` (primero Eureka y al final el gateway).

## Datos de ejemplo

Se cargan la primera vez que arranca cada servicio, si las tablas están vacías.

- Usuarios: `admin@artcrate.cl` / `Admin123` (ADMIN) y `cliente@artcrate.cl` / `Cliente123` (CLIENTE, id 2).
- Catálogo: 5 categorías y 12 productos (ids 1 a 12), con precios en CLP.
- Inventario: stock para los productos 1 a 12.

## Endpoints

Todas las rutas se llaman a través del gateway (`http://localhost:8080`).

### Usuarios

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/usuarios/registro` | Registrar cliente |
| POST | `/api/usuarios/login` | Iniciar sesión |
| GET | `/api/usuarios` | Listar usuarios |
| GET, PUT, DELETE | `/api/usuarios/{id}` | Obtener, actualizar o eliminar |

### Catálogo

| Método | Ruta | Descripción |
|---|---|---|
| GET, POST | `/api/categorias` | Listar o crear categorías |
| GET, PUT, DELETE | `/api/categorias/{id}` | Obtener, actualizar o eliminar |
| GET | `/api/productos?categoriaId=&q=` | Listar productos (filtros opcionales) |
| POST | `/api/productos` | Crear producto |
| GET, PUT, DELETE | `/api/productos/{id}` | Obtener, actualizar o eliminar |

### Inventario

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/inventario` | Stock de todos los productos |
| GET, PUT | `/api/inventario/{productoId}` | Ver o fijar el stock |
| POST | `/api/inventario/descontar` | Descontar una lista de productos (todo o nada) |
| POST | `/api/inventario/reponer` | Reponer stock |

### Carrito

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/carritos/{usuarioId}` | Ver el carrito |
| POST | `/api/carritos/{usuarioId}/items` | Agregar un producto |
| PUT | `/api/carritos/{usuarioId}/items/{productoId}` | Cambiar la cantidad |
| DELETE | `/api/carritos/{usuarioId}/items/{productoId}` | Quitar un producto |
| DELETE | `/api/carritos/{usuarioId}` | Vaciar el carrito |

### Pedidos

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/pedidos` | Crear pedido desde el carrito |
| GET | `/api/pedidos` | Listar pedidos |
| GET | `/api/pedidos/{id}` | Ver un pedido |
| GET | `/api/pedidos/usuario/{usuarioId}` | Pedidos de un usuario |
| PATCH | `/api/pedidos/{id}/estado` | Cambiar el estado |

### Pagos

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/pagos` | Pagar un pedido (`TARJETA` o `TRANSFERENCIA`) |
| GET | `/api/pagos/{id}` | Ver un pago |
| GET | `/api/pagos/pedido/{pedidoId}` | Pagos de un pedido |

La colección de Postman está en `docs/ArtCrate.postman_collection.json`. La carpeta "0 - Flujo de compra" recorre una compra completa.

## Reglas principales

- Los precios están en CLP, como números enteros.
- El carrito no permite superar el stock ni agregar productos inexistentes o inactivos.
- El stock se descuenta al crear el pedido, no al agregar al carrito.
- Estados del pedido: `PENDIENTE_PAGO`, `PAGADO`, `ENVIADO`, `ENTREGADO` y `CANCELADO` (solo desde `PENDIENTE_PAGO`, repone el stock).
- Solo se puede pagar un pedido en `PENDIENTE_PAGO`. El pago siempre se aprueba (es simulado).

## Errores

Todos los servicios y el gateway responden los errores con el mismo formato:

```json
{ "timestamp": "2026-10-07T23:47:09.944", "status": 409, "error": "Conflict", "message": "Stock insuficiente para el producto 3" }
```

Códigos usados: 201 al crear, 204 al borrar, 400 datos inválidos, 401 login incorrecto, 404 no existe, 409 conflicto y 503 cuando un servicio no está disponible.

## Estructura del repositorio

```
artcrate-backend/
├── pom.xml                  # agregador de los módulos
├── docker-compose.yml       # MySQL
├── docker/mysql-init/       # script que crea las bases
├── run-all.bat / run-all.sh / stop-all.sh
├── docs/                    # arquitectura y colección Postman
├── eureka-server/
├── gateway-service/
└── usuarios-service, catalogo-service, inventario-service,
    carrito-service, pedidos-service, pagos-service
```

## Notas

- No hay seguridad (JWT ni roles) ni pruebas automáticas; las pruebas se hicieron a mano con Postman y curl.
- Los servicios se registran en Eureka como `localhost`, así que las llamadas entre servicios no dependen del nombre del equipo.
- Los tiempos de Eureka están acortados para desarrollo. En producción conviene volver a los valores por defecto.
