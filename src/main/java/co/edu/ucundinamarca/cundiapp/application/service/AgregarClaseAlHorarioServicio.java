package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.AgregarClaseAlHorario;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio.AsignaturaEnCurso;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;

/**
 * Agrega a mano un bloque al horario (RF04, SCRUM-25).
 *
 * <p>Las reglas del bloque (día, horas, salón, docente) las comprueba el dominio al construirlo. Aquí
 * queda lo que el dominio no puede saber: que la asignatura sea del estudiante que pide y que la esté
 * cursando, porque el horario de una materia ya terminada no tiene a dónde ir.
 */
public class AgregarClaseAlHorarioServicio implements AgregarClaseAlHorario {

	private final HorarioRepositorio horarios;

	public AgregarClaseAlHorarioServicio(HorarioRepositorio horarios) {
		this.horarios = horarios;
	}

	@Override
	public ClaseDelHorario ejecutar(int idEstudiante, int idMatricula, DatosDeLaClase datos) {
		AsignaturaEnCurso asignatura = horarios.asignaturaEnCurso(idEstudiante, idMatricula)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"No encontramos esa asignatura entre las que estás cursando"));

		var bloque = new BloqueDeHorario(
				horarios.siguienteConsecutivo(idMatricula),
				datos.dia(), datos.horaInicio(), datos.horaFin(), datos.espacio(), datos.docente());
		horarios.guardar(idEstudiante, idMatricula, bloque);
		return new ClaseDelHorario(asignatura.idMatricula(), asignatura.codigoAsignatura(), asignatura.nombre(), bloque);
	}
}
