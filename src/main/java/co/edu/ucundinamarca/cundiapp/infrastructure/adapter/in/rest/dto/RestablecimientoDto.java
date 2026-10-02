package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Definir la contraseña nueva con el código recibido (SCRUM-68).
 * La política de la contraseña vive en el dominio ({@code Contrasena}), no aquí: es una regla de negocio.
 */
public record RestablecimientoDto(
		@NotBlank String correo,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "El código tiene 6 dígitos") String codigo,
		@NotBlank String contrasenaNueva) {
}
