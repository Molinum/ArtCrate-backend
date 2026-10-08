package cl.artcrate.inventario.config;

import cl.artcrate.inventario.model.Stock;
import cl.artcrate.inventario.repository.StockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Carga el stock de los productos 1..12 del catálogo al arrancar, solo si la tabla está vacía. */
@Configuration
public class DatosIniciales {

    @Bean
    CommandLineRunner cargarStock(StockRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return; // ya hay datos: no se duplica nada
            }
            // Posición en el arreglo = id del producto - 1
            int[] cantidades = {25, 14, 8, 40, 30, 15, 32, 22, 18, 20, 6, 10};
            for (int i = 0; i < cantidades.length; i++) {
                repository.save(new Stock((long) (i + 1), cantidades[i]));
            }
        };
    }
}
