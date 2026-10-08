package cl.artcrate.catalogo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos para crear o editar un producto. "activo" es opcional (por defecto, activo). */
public record ProductoRequest(
        @NotBlank(message = "El SKU es obligatorio")
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 1, message = "El precio debe ser un número entero mayor a 0")
        Long precio,

        String imagenUrl,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        Boolean activo) {
}
