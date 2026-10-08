package cl.artcrate.catalogo.service;

import cl.artcrate.catalogo.dto.ProductoRequest;
import cl.artcrate.catalogo.dto.ProductoResponse;
import cl.artcrate.catalogo.exception.ConflictoException;
import cl.artcrate.catalogo.exception.RecursoNoEncontradoException;
import cl.artcrate.catalogo.model.Categoria;
import cl.artcrate.catalogo.model.Producto;
import cl.artcrate.catalogo.repository.CategoriaRepository;
import cl.artcrate.catalogo.repository.ProductoRepository;
import java.text.Normalizer;
import java.util.List;
import org.springframework.stereotype.Service;

/** Lógica de negocio de los productos. */
@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository repository, CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
    }

    /** Lista los productos. Los dos filtros son opcionales y se pueden combinar. */
    public List<ProductoResponse> listar(Long categoriaId, String q) {
        List<Producto> productos = (categoriaId != null)
                ? repository.findByCategoriaId(categoriaId)
                : repository.findAll();

        // ?q= busca el texto (sin importar mayúsculas ni tildes) en el nombre o en la descripción
        if (q != null && !q.isBlank()) {
            String texto = normalizar(q);
            productos = productos.stream()
                    .filter(p -> contiene(p.getNombre(), texto) || contiene(p.getDescripcion(), texto))
                    .toList();
        }
        return productos.stream().map(this::aRespuesta).toList();
    }

    public ProductoResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public ProductoResponse crear(ProductoRequest datos) {
        String sku = datos.sku().trim();
        if (repository.existsBySku(sku)) {
            throw new ConflictoException("Ya existe un producto con el SKU " + sku);
        }
        Producto producto = new Producto();
        copiarDatos(producto, datos);
        return aRespuesta(repository.save(producto));
    }

    public ProductoResponse actualizar(Long id, ProductoRequest datos) {
        Producto producto = buscar(id);
        String sku = datos.sku().trim();
        // Solo es conflicto si el SKU nuevo ya lo usa OTRO producto
        if (!producto.getSku().equals(sku) && repository.existsBySku(sku)) {
            throw new ConflictoException("Ya existe un producto con el SKU " + sku);
        }
        copiarDatos(producto, datos);
        return aRespuesta(repository.save(producto));
    }

    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    // ----- métodos de apoyo -----

    private Producto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + id));
    }

    /** Pasa los datos del request a la entidad (sirve tanto para crear como para editar). */
    private void copiarDatos(Producto producto, ProductoRequest datos) {
        Categoria categoria = categoriaRepository.findById(datos.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoría con id " + datos.categoriaId()));

        producto.setSku(datos.sku().trim());
        producto.setNombre(datos.nombre().trim());
        producto.setDescripcion(datos.descripcion());
        producto.setPrecio(datos.precio());
        producto.setImagenUrl(datos.imagenUrl());
        producto.setCategoria(categoria);
        // Si "activo" no viene en el request: al crear queda activo y al editar no cambia
        if (datos.activo() != null) {
            producto.setActivo(datos.activo());
        }
    }

    private boolean contiene(String texto, String buscado) {
        return texto != null && normalizar(texto).contains(buscado);
    }

    /** Pasa a minúsculas y quita las tildes: "Acrílico" y "acrilico" quedan iguales. */
    private String normalizar(String texto) {
        // NFD separa la letra de su tilde ("í" -> "i" + tilde); luego se borran las tildes
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().toLowerCase();
    }

    private ProductoResponse aRespuesta(Producto p) {
        return new ProductoResponse(p.getId(), p.getSku(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.getImagenUrl(), p.getCategoria().getId(), p.getCategoria().getNombre(), p.isActivo());
    }
}
