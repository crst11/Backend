package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.Pendiente;
import co.edu.ucundinamarca.cundiapp.domain.model.PendienteConSituacion;
import java.time.Instant;
import java.util.Locale;

/**
 * Un pendiente como lo ve la pantalla (RF06, SCRUM-29 y SCRUM-30).
 *
 * @param situacion {@code vencido}, {@code urgente} (vence en menos de 48 horas), {@code proximo} o
 *     {@code cumplido}: la calcula el servidor con su hora, no el teléfono
 */
public record PendienteDto(
		int id,
		String titulo,
		String descripcion,
		Instant fechaLimite,
		String prioridad,
		String estado,
		String situacion,
		Integer idMatricula) {

	public static PendienteDto desde(PendienteConSituacion visto) {
		Pendiente p = visto.pendiente();
		return new PendienteDto(p.id(), p.titulo(), p.descripcion(), p.fechaLimite(), p.prioridad().valorEnBd(),
				p.estado().valorEnBd(), visto.situacion().name().toLowerCase(Locale.ROOT), p.idMatricula());
	}
}
