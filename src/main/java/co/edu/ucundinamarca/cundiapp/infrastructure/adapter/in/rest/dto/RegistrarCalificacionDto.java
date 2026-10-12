package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * La nota que manda la pantalla para una actividad (SCRUM-28).
 *
 * <p>Solo se exige que venga: que esté entre 0.0 y 5.0 y que no pase de dos decimales es una regla
 * del negocio y la comprueba el dominio, para que el mensaje al estudiante sea el mismo en cualquier
 * ruta por la que llegue.
 */
public record RegistrarCalificacionDto(@NotNull BigDecimal nota) {
}
