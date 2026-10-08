package cl.artcrate.usuarios.dto;

import cl.artcrate.usuarios.model.Rol;

/** Lo que se devuelve al cliente: el usuario SIN la contraseña. */
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        String region,
        Rol rol,
        boolean activo) {
}
