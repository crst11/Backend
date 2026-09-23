package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Validación de formato (Bean Validation); las reglas de negocio las aplica el dominio. */
public record RegistroDto(
		@NotBlank String correo,
		@NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String contrasena,
		@NotBlank String nombres,
		@NotBlank String apellidos,
		boolean aceptaTratamientoDatos) {
}
