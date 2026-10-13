package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiDia;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AgendaDelDia;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.CalendarioAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.HorarioSemanal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * La pantalla del día (SCRUM-31 y SCRUM-32). Qué clases tocan y cuál va es una regla del dominio
 * ({@link AgendaDelDia}); aquí se juntan el horario, el calendario y la hora de Bogotá.
 */
public class ConsultarMiDiaServicio implements ConsultarMiDia {

	private final HorarioRepositorio horarios;
	private final EstructuraDeEvaluacionRepositorio evaluaciones;
	private final Clock reloj;
	private final CalendarioAcademico calendario = new CalendarioAcademico();

	public ConsultarMiDiaServicio(
			HorarioRepositorio horarios, EstructuraDeEvaluacionRepositorio evaluaciones, Clock reloj) {
		this.horarios = horarios;
		this.evaluaciones = evaluaciones;
		this.reloj = reloj;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, LocalDate fecha) {
		LocalDateTime ahora = LocalDateTime.now(reloj);
		LocalDate dia = fecha == null ? ahora.toLocalDate() : fecha;
		var horario = new HorarioSemanal(horarios.clasesDe(idEstudiante));
		var agenda = AgendaDelDia.de(dia, ahora, horario, calendario);
		var actividades = evaluaciones.matriculasDe(idEstudiante).stream()
				.collect(Collectors.toMap(
						AsignaturaMatriculada::idMatricula, AsignaturaMatriculada::actividadesDefinidas, Integer::sum));
		return new Resultado(ahora.toLocalDate(), agenda, actividades);
	}
}
