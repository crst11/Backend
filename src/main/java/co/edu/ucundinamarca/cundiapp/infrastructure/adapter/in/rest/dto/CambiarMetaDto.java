package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * La meta nueva del estudiante (SCRUM-34). Solo se exige que venga: que esté entre 0.0 y 5.0 y no
 * pase de dos decimales lo comprueba el dominio.
 */
public record CambiarMetaDto(@NotNull BigDecimal meta) {
}
