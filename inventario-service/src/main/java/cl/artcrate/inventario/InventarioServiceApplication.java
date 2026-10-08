package cl.artcrate.inventario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del servicio de inventario (puerto 8083). */
@SpringBootApplication
public class InventarioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventarioServiceApplication.class, args);
    }
}
