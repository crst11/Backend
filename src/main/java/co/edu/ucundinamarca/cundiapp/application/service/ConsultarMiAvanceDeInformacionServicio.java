package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiAvanceDeInformacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AvanceDeInformacion;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeImportacion;

/**
 * El avance sale de lo que ya existe, sin guardar nada nuevo (SCRUM-84): una importación confirmada del
 * Registro Académico Extendido, clases en el horario y actividades en cada asignatura en curso. Qué
 * cuenta como avance es una regla del dominio ({@link AvanceDeInformacion}).
 */
public class ConsultarMiAvanceDeInformacionServicio implements ConsultarMiAvanceDeInformacion {

	private static final String TIPO_REGISTRO_EXTENDIDO = "historial";

	private final ImportacionRepositorio importaciones;
	private final HorarioRepositorio horarios;
	private final EstructuraDeEvaluacionRepositorio evaluaciones;

	public ConsultarMiAvanceDeInformacionServicio(
			ImportacionRepositorio importaciones,
			HorarioRepositorio horarios,
			EstructuraDeEvaluacionRepositorio evaluaciones) {
		this.importaciones = importaciones;
		this.horarios = horarios;
		this.evaluaciones = evaluaciones;
	}

	@Override
	public AvanceDeInformacion ejecutar(int idEstudiante) {
		boolean registro = importaciones.deEstudiante(idEstudiante).stream()
				.anyMatch(carga -> TIPO_REGISTRO_EXTENDIDO.equals(carga.tipoReporte())
						&& carga.estado() == EstadoDeImportacion.CONFIRMADA);
		// Una actividad extra (baile, semillero) no es el reporte de horario: no cuenta como cargado.
		boolean horario = horarios.clasesDe(idEstudiante).stream().anyMatch(clase -> !clase.esExtra());
		return AvanceDeInformacion.de(registro, horario, evaluaciones.matriculasDe(idEstudiante));
	}
}
