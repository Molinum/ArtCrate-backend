package cl.artcrate.usuarios.dto;

import cl.artcrate.usuarios.model.Rol;
import jakarta.validation.constraints.NotBlank;

/** Datos que se pueden modificar de un usuario. Rol y activo son opcionales (si vienen vacíos no cambian). */
public record ActualizarUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        String telefono,

        String region,

        Rol rol,

        Boolean activo) {
}
