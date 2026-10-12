package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Cómo se identifica una actividad dentro de su matrícula: el número de orden de su categoría y el
 * suyo propio dentro de ella (así está la clave en el esquema).
 */
public record ClaveDeActividad(int consecCategoria, int consecActividad) {
}
