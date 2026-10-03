package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ElegirProgramaDto(@NotBlank String codigoPrograma) {
}
