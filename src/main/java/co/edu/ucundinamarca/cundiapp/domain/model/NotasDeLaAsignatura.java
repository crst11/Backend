package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.Map;
import java.util.Optional;

/**
 * Las notas que el estudiante ha registrado en una asignatura matriculada (RF05, SCRUM-28), por
 * actividad. Una actividad sin nota simplemente no está en el mapa.
 */
public record NotasDeLaAsignatura(int idMatricula, Map<ClaveDeActividad, Nota> notas) {

	public NotasDeLaAsignatura {
		notas = notas == null ? Map.of() : Map.copyOf(notas);
	}

	public Optional<Nota> de(int consecCategoria, int consecActividad) {
		return Optional.ofNullable(notas.get(new ClaveDeActividad(consecCategoria, consecActividad)));
	}
}
