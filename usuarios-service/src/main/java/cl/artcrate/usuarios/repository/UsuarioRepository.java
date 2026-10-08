package cl.artcrate.usuarios.repository;

import cl.artcrate.usuarios.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring Data crea la consulta a partir del nombre del método
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
