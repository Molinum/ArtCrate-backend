package cl.artcrate.usuarios.config;

import cl.artcrate.usuarios.model.Rol;
import cl.artcrate.usuarios.model.Usuario;
import cl.artcrate.usuarios.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Carga dos usuarios de prueba al arrancar, solo si la tabla está vacía. */
@Configuration
public class DatosIniciales {

    @Bean
    CommandLineRunner cargarUsuarios(UsuarioRepository repository, PasswordEncoder encoder) {
        return args -> {
            if (repository.count() > 0) {
                return; // ya hay datos: no se duplica nada
            }
            repository.save(crear("Admin", "ArtCrate", "admin@artcrate.cl", "Admin123", Rol.ADMIN, encoder));
            repository.save(crear("Cliente", "Prueba", "cliente@artcrate.cl", "Cliente123", Rol.CLIENTE, encoder));
        };
    }

    private Usuario crear(String nombre, String apellido, String correo, String password, Rol rol,
            PasswordEncoder encoder) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setCorreo(correo);
        u.setPasswordHash(encoder.encode(password));
        u.setTelefono("+56912345678");
        u.setRegion("Metropolitana de Santiago");
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }
}
