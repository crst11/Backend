package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ReenvioDto(@NotBlank String correo) {
}
