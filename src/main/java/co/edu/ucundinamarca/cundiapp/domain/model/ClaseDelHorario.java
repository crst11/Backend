package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Un bloque del horario junto con la asignatura a la que pertenece (RF04, SCRUM-25): lo que se ve
 * en una fila de la pantalla de horario.
 */
public record ClaseDelHorario(int idMatricula, String codigoAsignatura, String asignatura, BloqueDeHorario bloque) {
}
