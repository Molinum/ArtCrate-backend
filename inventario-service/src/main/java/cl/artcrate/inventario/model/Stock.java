package cl.artcrate.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Stock disponible de un producto. Su clave es el id del producto (el mismo del catálogo). */
@Entity
@Table(name = "stock")
public class Stock {

    // Sin @GeneratedValue: el id lo ponemos nosotros (es el id del producto en catalogo-service)
    @Id
    private Long productoId;

    @Column(nullable = false)
    private int cantidad;

    public Stock() {
    }

    public Stock(Long productoId, int cantidad) {
        this.productoId = productoId;
        this.cantidad = cantidad;
    }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
