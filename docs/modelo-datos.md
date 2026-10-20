# Modelo de datos de ArtCrate

Una base de datos MySQL por servicio. Las líneas sólidas son claves foráneas reales (dentro de una misma base);
las punteadas son referencias por id entre servicios (sin clave foránea, porque cada servicio tiene su propia base).

```mermaid
classDiagram
    direction LR

    namespace usuarios_db {
        class Usuario {
            Long id
            String nombre
            String apellido
            String correo «único»
            String passwordHash
            String telefono
            String region
            Rol rol
            boolean activo
        }
        class Rol {
            <<enumeration>>
            CLIENTE
            ADMIN
        }
    }

    namespace catalogo_db {
        class Categoria {
            Long id
            String nombre «único»
            String descripcion
        }
        class Producto {
            Long id
            String sku «único»
            String nombre
            String descripcion
            long precio
            String imagenUrl
            boolean activo
        }
    }

    namespace inventario_db {
        class Stock {
            Long productoId
            int cantidad
        }
    }

    namespace carrito_db {
        class Carrito {
            Long id
            Long usuarioId «único»
        }
        class ItemCarrito {
            Long id
            Long productoId
            String nombre
            long precioUnitario
            int cantidad
            String imagenUrl
        }
    }

    namespace pedidos_db {
        class Pedido {
            Long id
            Long usuarioId
            LocalDateTime fecha
            EstadoPedido estado
            long total
            String direccionDespacho
            String comuna
            String region
        }
        class ItemPedido {
            Long id
            Long productoId
            String nombre
            long precioUnitario
            int cantidad
            long subtotal
        }
        class EstadoPedido {
            <<enumeration>>
            PENDIENTE_PAGO
            PAGADO
            ENVIADO
            ENTREGADO
            CANCELADO
        }
    }

    namespace pagos_db {
        class Pago {
            Long id
            Long pedidoId
            long monto
            MetodoPago metodo
            EstadoPago estado
            LocalDateTime fecha
            String codigoTransaccion «único»
        }
        class MetodoPago {
            <<enumeration>>
            TARJETA
            TRANSFERENCIA
        }
        class EstadoPago {
            <<enumeration>>
            APROBADO
            RECHAZADO
        }
    }

    %% Claves foráneas reales (dentro de cada servicio)
    Categoria "1" <-- "*" Producto : categoria_id
    Carrito "1" *-- "*" ItemCarrito : carrito_id
    Pedido "1" *-- "*" ItemPedido : pedido_id

    %% Enumeraciones
    Usuario --> Rol
    Pedido --> EstadoPedido
    Pago --> MetodoPago
    Pago --> EstadoPago

    %% Referencias por id entre servicios (sin clave foránea)
    Usuario "1" <.. "0..1" Carrito : usuarioId
    Usuario "1" <.. "*" Pedido : usuarioId
    Producto "1" <.. "0..1" Stock : productoId
    Producto "1" <.. "*" ItemCarrito : productoId
    Producto "1" <.. "*" ItemPedido : productoId
    Pedido "1" <.. "*" Pago : pedidoId
```
