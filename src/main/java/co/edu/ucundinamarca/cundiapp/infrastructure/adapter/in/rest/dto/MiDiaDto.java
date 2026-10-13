package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiDia.Resultado;
import co.edu.ucundinamarca.cundiapp.domain.model.AgendaDelDia;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeLaClase;
import java.util.List;

/**
 * La pantalla del día como la lee el teléfono (SCRUM-31 y SCRUM-32). `hoy` es la fecha de Bogotá;
 * `fecha` es la que se muestra, que es hoy salvo que se haya pedido otra. `tipo` es `lectivo`,
 * `festivo` o `fuera_de_periodo`, y `festivo` solo trae el nombre cuando lo es. `siguiente` es el próximo
 * día con clases y no viene si el estudiante no tiene ninguna.
 */
public record MiDiaDto(
		String hoy,
		String fecha,
		String dia,
		String tipo,
		String festivo,
		List<ClaseDelDiaDto> clases,
		SiguienteDto siguiente) {

	/** `estado` solo cambia en hoy: `pasada`, `en_curso`, `proxima` o `luego`. */
	public record ClaseDelDiaDto(
			int idMatricula,
			String codigoAsignatura,
			String asignatura,
			String horaInicio,
			String horaFin,
			String salon,
			String bloque,
			String sede,
			HorarioDto.DocenteDto docente,
			String estado,
			int actividades) {

		static ClaseDelDiaDto desde(ClaseDelHorario clase, EstadoDeLaClase estado, int actividades) {
			var bloque = clase.bloque();
			var espacio = bloque.espacio().orElse(null);
			return new ClaseDelDiaDto(
					clase.idMatricula(),
					clase.codigoAsignatura(),
					clase.asignatura(),
					bloque.horaInicio().toString(),
					bloque.horaFin().toString(),
					espacio == null ? null : espacio.salon(),
					espacio == null ? null : espacio.bloque(),
					espacio == null ? null : espacio.sede(),
					bloque.docente().map(d -> new HorarioDto.DocenteDto(d.nombres(), d.apellidos())).orElse(null),
					estado.valor(),
					actividades);
		}
	}

	public record SiguienteDto(String fecha, String dia, List<ClaseDelDiaDto> clases) {
	}

	public static MiDiaDto desde(Resultado resultado) {
		AgendaDelDia agenda = resultado.agenda();
		var dia = agenda.dia();
		return new MiDiaDto(
				resultado.hoy().toString(),
				dia.fecha().toString(),
				dia.diaDeLaSemana().valorEnBd(),
				dia.tipo().valor(),
				dia.nombreDelFestivo(),
				agenda.clases().stream()
						.map(c -> ClaseDelDiaDto.desde(c.clase(), c.estado(), resultado.actividadesDe(c.clase().idMatricula())))
						.toList(),
				agenda.siguiente()
						.map(s -> new SiguienteDto(
								s.dia().fecha().toString(),
								s.dia().diaDeLaSemana().valorEnBd(),
								s.clases().stream()
										.map(c -> ClaseDelDiaDto.desde(c, EstadoDeLaClase.LUEGO, resultado.actividadesDe(c.idMatricula())))
										.toList()))
						.orElse(null));
	}
}
