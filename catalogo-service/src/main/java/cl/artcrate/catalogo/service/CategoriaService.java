package cl.artcrate.catalogo.service;

import cl.artcrate.catalogo.dto.CategoriaRequest;
import cl.artcrate.catalogo.dto.CategoriaResponse;
import cl.artcrate.catalogo.exception.ConflictoException;
import cl.artcrate.catalogo.exception.RecursoNoEncontradoException;
import cl.artcrate.catalogo.model.Categoria;
import cl.artcrate.catalogo.repository.CategoriaRepository;
import cl.artcrate.catalogo.repository.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Lógica de negocio de las categorías. */
@Service
public class CategoriaService {

    private final CategoriaRepository repository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository repository, ProductoRepository productoRepository) {
        this.repository = repository;
        this.productoRepository = productoRepository;
    }

    public List<CategoriaResponse> listar() {
        return repository.findAll().stream().map(this::aRespuesta).toList();
    }

    public CategoriaResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public CategoriaResponse crear(CategoriaRequest datos) {
        String nombre = datos.nombre().trim();
        if (repository.existsByNombre(nombre)) {
            throw new ConflictoException("Ya existe una categoría llamada " + nombre);
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(datos.descripcion());
        return aRespuesta(repository.save(categoria));
    }

    public CategoriaResponse actualizar(Long id, CategoriaRequest datos) {
        Categoria categoria = buscar(id);
        String nombre = datos.nombre().trim();
        // Solo es conflicto si el nombre nuevo ya lo usa OTRA categoría
        if (!categoria.getNombre().equals(nombre) && repository.existsByNombre(nombre)) {
            throw new ConflictoException("Ya existe una categoría llamada " + nombre);
        }
        categoria.setNombre(nombre);
        categoria.setDescripcion(datos.descripcion());
        return aRespuesta(repository.save(categoria));
    }

    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede eliminar la categoría porque tiene productos asociados");
        }
        repository.delete(categoria);
    }

    // ----- métodos de apoyo -----

    private Categoria buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría con id " + id));
    }

    private CategoriaResponse aRespuesta(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion());
    }
}
