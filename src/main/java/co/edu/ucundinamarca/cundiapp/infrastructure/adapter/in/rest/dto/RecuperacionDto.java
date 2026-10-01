package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** Pedir el código para cambiar la contraseña (SCRUM-68). */
public record RecuperacionDto(@NotBlank String correo) {
}
