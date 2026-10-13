package co.edu.ucundinamarca.cundiapp.domain.model;

/** Una clase del día y dónde está respecto de la hora de ahora (RF07, SCRUM-31 y SCRUM-32). */
public record ClaseDeLaAgenda(ClaseDelHorario clase, EstadoDeLaClase estado) {
}
