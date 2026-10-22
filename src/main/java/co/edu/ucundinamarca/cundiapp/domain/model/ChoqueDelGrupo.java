package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.LocalTime;

/**
 * La clase del horario armado con la que choca un grupo candidato (RF13, SCRUM-245): la materia y el grupo de la
 * <b>otra</b> clase, con su día y su hora, para que la persona sepa exactamente qué estorba.
 */
public record ChoqueDelGrupo(String asignatura, String grupo, DiaDeLaSemana dia, LocalTime inicio, LocalTime fin) {
}
