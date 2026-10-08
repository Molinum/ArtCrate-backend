package cl.artcrate.catalogo.config;

import cl.artcrate.catalogo.model.Categoria;
import cl.artcrate.catalogo.model.Producto;
import cl.artcrate.catalogo.repository.CategoriaRepository;
import cl.artcrate.catalogo.repository.ProductoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Carga 5 categorías y 12 productos al arrancar, solo si las tablas están vacías.
 * Los productos se guardan en orden para que sus ids sean 1..12 (el inventario depende de eso).
 */
@Configuration
public class DatosIniciales {

    @Bean
    CommandLineRunner cargarCatalogo(CategoriaRepository categorias, ProductoRepository productos) {
        return args -> {
            if (categorias.count() == 0) {
                categorias.save(categoria("Pintura", "Acrílicos, óleos y acuarelas"));
                categorias.save(categoria("Dibujo", "Lápices, carboncillo y marcadores"));
                categorias.save(categoria("Papeles y lienzos", "Soportes para pintar y dibujar"));
                categorias.save(categoria("Pinceles y accesorios", "Pinceles, caballetes y complementos"));
                categorias.save(categoria("Kits ArtCrate", "Kits listos para empezar a crear"));
            }

            if (productos.count() == 0) {
                List<Categoria> cats = categorias.findAll(Sort.by("id")); // en orden: 1..5
                Categoria pintura = cats.get(0);
                Categoria dibujo = cats.get(1);
                Categoria papeles = cats.get(2);
                Categoria pinceles = cats.get(3);
                Categoria kits = cats.get(4);

                productos.save(producto("ART-PIN-001", "Acrílico 12 colores",
                        "Set de 12 tubos de pintura acrílica de 22 ml, secado rápido y colores intensos.",
                        12990, "assets/img/producto-01-acrilico-12-colores.svg", pintura));
                productos.save(producto("ART-PIN-002", "Óleo 6 tubos",
                        "Seis tubos de pintura al óleo de 20 ml con gran poder cubriente.",
                        18990, "assets/img/producto-02-oleo-6-tubos.svg", pintura));
                productos.save(producto("ART-PIN-003", "Acuarela 24 pastillas",
                        "Caja metálica de acuarela profesional con 24 pastillas y pincel.",
                        24990, "assets/img/producto-03-acuarela-24-pastillas.svg", pintura));
                productos.save(producto("ART-DIB-001", "Lápices grafito x12",
                        "Set de 12 lápices de grafito de distintas durezas (de 6B a 4H).",
                        7990, "assets/img/producto-04-lapices-grafito-12.svg", dibujo));
                productos.save(producto("ART-DIB-002", "Carboncillo y difuminos",
                        "Barras de carboncillo natural con difuminos de papel para sombreado.",
                        5990, "assets/img/producto-05-carboncillo-difuminos.svg", dibujo));
                productos.save(producto("ART-DIB-003", "Marcadores x24",
                        "Estuche de 24 marcadores de doble punta, fina y gruesa.",
                        16990, "assets/img/producto-06-marcadores-24.svg", dibujo));
                productos.save(producto("ART-PAP-001", "Lienzo 40x50 cm",
                        "Lienzo de algodón imprimado de 40 por 50 centímetros, listo para pintar.",
                        8990, "assets/img/producto-07-lienzo-40x50.svg", papeles));
                productos.save(producto("ART-PAP-002", "Block acuarela A4",
                        "Block de 20 hojas de papel para acuarela de 300 gramos, tamaño A4.",
                        9990, "assets/img/producto-08-block-acuarela-a4.svg", papeles));
                productos.save(producto("ART-PAP-003", "Cuaderno de bocetos A5",
                        "Cuaderno de bocetos de tapa dura, 80 hojas, tamaño A5.",
                        6490, "assets/img/producto-09-cuaderno-bocetos-a5.svg", papeles));
                productos.save(producto("ART-ACC-001", "Set de 5 pinceles",
                        "Cinco pinceles sintéticos de distintas formas y tamaños.",
                        7490, "assets/img/producto-10-set-pinceles-5.svg", pinceles));
                productos.save(producto("ART-ACC-002", "Caballete de mesa",
                        "Caballete de mesa de madera plegable, ideal para espacios pequeños.",
                        21990, "assets/img/producto-11-caballete-mesa.svg", pinceles));
                productos.save(producto("ART-KIT-001", "Kit Iniciación Acrílico",
                        "Kit ArtCrate con pinturas acrílicas, pinceles, lienzo y paleta para empezar.",
                        29990, "assets/img/producto-12-kit-iniciacion-acrilico.svg", kits));
            }
        };
    }

    private Categoria categoria(String nombre, String descripcion) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        return c;
    }

    private Producto producto(String sku, String nombre, String descripcion, long precio, String imagenUrl,
            Categoria categoria) {
        Producto p = new Producto();
        p.setSku(sku);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setImagenUrl(imagenUrl);
        p.setCategoria(categoria);
        p.setActivo(true);
        return p;
    }
}
