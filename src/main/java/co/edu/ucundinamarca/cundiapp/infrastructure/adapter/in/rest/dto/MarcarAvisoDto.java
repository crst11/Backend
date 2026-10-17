package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

/** Si el aviso quedó leído o vuelve a sin leer (SCRUM-37). */
public record MarcarAvisoDto(@NotNull Boolean leido) {
}
