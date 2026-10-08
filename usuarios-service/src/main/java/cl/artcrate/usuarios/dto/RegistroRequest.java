package cl.artcrate.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Datos que envía el cliente para registrarse. */
public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        @NotBlank(message = "El correo es obligatorio")
        @Email(regexp = ".+@.+\\..+", message = "El correo no tiene un formato válido (ej: nombre@dominio.cl)")
        String correo,

        // (?=.*[A-Z]) exige una mayúscula, (?=.*[0-9]) un número y .{8,} al menos 8 caracteres
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
                 message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número")
        String password,

        String telefono,

        String region) {
}
