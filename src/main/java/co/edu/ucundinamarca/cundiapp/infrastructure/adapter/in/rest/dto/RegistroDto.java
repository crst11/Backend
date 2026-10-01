package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Validación de formato (Bean Validation); las reglas de negocio las aplica el dominio.
 * La política de la contraseña vive en {@code Contrasena}, no aquí: es una regla de negocio.
 */
public record RegistroDto(
		@NotBlank String correo,
		@NotBlank String contrasena,
		@NotBlank String nombres,
		@NotBlank String apellidos,
		boolean aceptaTratamientoDatos) {
}
