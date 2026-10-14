package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.AvanceDeInformacion;
import co.edu.ucundinamarca.cundiapp.domain.model.PasoDeAvance;
import java.util.List;

/**
 * Lo que lleva cargado el estudiante y lo que le falta (SCRUM-84). `tipo` es `registro_extendido`,
 * `horario` o `asignatura`; `idMatricula` solo viene en los pasos de una asignatura.
 */
public record AvanceDeInformacionDto(int porcentaje, int hechos, int total, List<PasoDto> pasos) {

	public record PasoDto(String tipo, String texto, boolean hecho, Integer idMatricula) {

		static PasoDto desde(PasoDeAvance paso) {
			return new PasoDto(paso.tipo().valor(), paso.texto(), paso.hecho(), paso.idMatricula());
		}
	}

	public static AvanceDeInformacionDto desde(AvanceDeInformacion avance) {
		return new AvanceDeInformacionDto(
				avance.porcentaje(), avance.hechos(), avance.total(), avance.pasos().stream().map(PasoDto::desde).toList());
	}
}
