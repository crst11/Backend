package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificacionDto(
		@NotBlank String correo,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "El código tiene 6 dígitos") String codigo) {
}
