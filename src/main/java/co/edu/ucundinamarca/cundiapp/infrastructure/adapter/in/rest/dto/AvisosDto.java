package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisAvisos;
import java.time.Instant;
import java.util.List;

/**
 * Mis avisos (RF10, SCRUM-37). `sinLeer` cuenta todos los no leídos, aunque la lista venga recortada; es
 * lo que muestra la campana. `tipo` es {@code vencimiento_proximo}, {@code pendiente_vencido} o {@code cambio_riesgo}.
 */
public record AvisosDto(int sinLeer, List<AvisoDto> avisos) {

	public record AvisoDto(int id, String tipo, String titulo, String mensaje, Instant fecha, boolean leido) {
	}

	public static AvisosDto desde(ListarMisAvisos.Resultado resultado) {
		return new AvisosDto(resultado.sinLeer(), resultado.avisos().stream()
				.map(aviso -> new AvisoDto(aviso.id(), aviso.tipo().valorEnBd(), aviso.titulo(), aviso.mensaje(),
						aviso.fechaGeneracion(), aviso.leido()))
				.toList());
	}
}
