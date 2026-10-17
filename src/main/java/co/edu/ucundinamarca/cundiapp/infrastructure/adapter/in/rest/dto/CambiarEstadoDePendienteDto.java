package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

/** Marcar un pendiente como hecho ({@code true}) o volver a abrirlo ({@code false}) (SCRUM-29). */
public record CambiarEstadoDePendienteDto(@NotNull Boolean cumplido) {
}
