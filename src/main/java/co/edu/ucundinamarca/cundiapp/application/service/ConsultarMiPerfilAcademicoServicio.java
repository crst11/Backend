package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiPerfilAcademico;
import co.edu.ucundinamarca.cundiapp.application.port.in.MiPerfilAcademico;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;

/** Mientras el estudiante no elija programa, el perfil lo dice en vez de fallar (RF02, SCRUM-21). */
public class ConsultarMiPerfilAcademicoServicio implements ConsultarMiPerfilAcademico {

	private final EstudianteRepositorio estudiantes;
	private final ProgramaAcademicoRepositorio programas;

	public ConsultarMiPerfilAcademicoServicio(
			EstudianteRepositorio estudiantes, ProgramaAcademicoRepositorio programas) {
		this.estudiantes = estudiantes;
		this.programas = programas;
	}

	@Override
	public MiPerfilAcademico ejecutar(int idEstudiante) {
		return new MiPerfilAcademico(estudiantes.planDe(idEstudiante).flatMap(programas::buscarPlanPorCodigo));
	}
}
