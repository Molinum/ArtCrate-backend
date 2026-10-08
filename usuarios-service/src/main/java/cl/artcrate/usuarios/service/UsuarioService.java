package cl.artcrate.usuarios.service;

import cl.artcrate.usuarios.dto.ActualizarUsuarioRequest;
import cl.artcrate.usuarios.dto.LoginRequest;
import cl.artcrate.usuarios.dto.RegistroRequest;
import cl.artcrate.usuarios.dto.UsuarioResponse;
import cl.artcrate.usuarios.exception.ConflictoException;
import cl.artcrate.usuarios.exception.CredencialesInvalidasException;
import cl.artcrate.usuarios.exception.RecursoNoEncontradoException;
import cl.artcrate.usuarios.model.Rol;
import cl.artcrate.usuarios.model.Usuario;
import cl.artcrate.usuarios.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Lógica de negocio de los usuarios. */
@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponse registrar(RegistroRequest datos) {
        String correo = normalizar(datos.correo());
        if (repository.existsByCorreo(correo)) {
            throw new ConflictoException("Ya existe un usuario con el correo " + correo);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(datos.password()));
        usuario.setTelefono(datos.telefono());
        usuario.setRegion(datos.region());
        usuario.setRol(Rol.CLIENTE); // quien se registra desde la tienda siempre es cliente
        return aRespuesta(repository.save(usuario));
    }

    public UsuarioResponse login(LoginRequest datos) {
        // El mismo mensaje para correo inexistente y contraseña errónea: no se revela cuál falló
        Usuario usuario = repository.findByCorreo(normalizar(datos.correo()))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        boolean passwordCorrecta = passwordEncoder.matches(datos.password(), usuario.getPasswordHash());
        if (!passwordCorrecta || !usuario.isActivo()) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }
        return aRespuesta(usuario);
    }

    public List<UsuarioResponse> listar() {
        return repository.findAll().stream().map(this::aRespuesta).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest datos) {
        Usuario usuario = buscar(id);
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setTelefono(datos.telefono());
        usuario.setRegion(datos.region());
        if (datos.rol() != null) {
            usuario.setRol(datos.rol());
        }
        if (datos.activo() != null) {
            usuario.setActivo(datos.activo());
        }
        return aRespuesta(repository.save(usuario));
    }

    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    // ----- métodos de apoyo -----

    private Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
    }

    /** El correo se guarda en minúsculas y sin espacios para que no haya duplicados disfrazados. */
    private String normalizar(String correo) {
        return correo.trim().toLowerCase();
    }

    /** Convierte la entidad en el DTO de respuesta (sin passwordHash). */
    private UsuarioResponse aRespuesta(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(),
                u.getTelefono(), u.getRegion(), u.getRol(), u.isActivo());
    }
}
