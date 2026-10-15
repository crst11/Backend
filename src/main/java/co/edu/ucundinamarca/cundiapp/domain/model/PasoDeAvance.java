package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Un paso del avance de información: qué falta o qué ya está cargado (RF03, SCRUM-84). `idMatricula`
 * solo viene en los pasos de una asignatura.
 */
public record PasoDeAvance(TipoDePaso tipo, String texto, boolean hecho, Integer idMatricula) {
}
