package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(@NotBlank String correo, @NotBlank String contrasena) {
}
