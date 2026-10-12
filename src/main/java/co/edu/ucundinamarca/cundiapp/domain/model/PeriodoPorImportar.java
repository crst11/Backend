package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.List;

/** Un período detectado con sus asignaturas, listo para que el estudiante lo confirme (SCRUM-23). */
public record PeriodoPorImportar(
		String codigo, List<AsignaturaPorImportar> asignaturas, ResumenOficialDePeriodo oficial) {

	public PeriodoPorImportar {
		asignaturas = asignaturas == null ? List.of() : List.copyOf(asignaturas);
	}

	public long cuantasSeVanAGuardar() {
		return asignaturas.stream().filter(AsignaturaPorImportar::seVaAGuardar).count();
	}
}
