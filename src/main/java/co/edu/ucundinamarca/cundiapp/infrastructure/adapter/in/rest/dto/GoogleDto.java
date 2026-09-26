package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** El ID token que Google Identity Services le entrega al frontend ("credential"). */
public record GoogleDto(@NotBlank(message = "Falta el token de Google") String idToken) {
}
