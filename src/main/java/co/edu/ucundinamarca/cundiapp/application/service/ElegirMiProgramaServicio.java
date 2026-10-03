package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ElegirMiPrograma;
import co.edu.ucundinamarca.cundiapp.application.port.in.MiPerfilAcademico;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import java.util.Optional;

/**
 * Elegir programa no es un dato suelto: deja fijado el plan contra el que después se comparan el
 * historial y el avance de carrera. Por eso se guarda el plan vigente y no solo el programa.
 */
public class ElegirMiProgramaServicio implements ElegirMiPrograma {

	private final EstudianteRepositorio estudiantes;
	private final ProgramaAcademicoRepositorio programas;

	public ElegirMiProgramaServicio(EstudianteRepositorio estudiantes, ProgramaAcademicoRepositorio programas) {
		this.estudiantes = estudiantes;
		this.programas = programas;
	}

	@Override
	public MiPerfilAcademico ejecutar(int idEstudiante, String codigoPrograma) {
		PlanDeEstudios plan = programas.buscarPlanVigenteDe(codigoPrograma)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(ConsultarPlanDeEstudiosServicio.SIN_RUTA));
		estudiantes.asignarPlan(idEstudiante, plan.codigo());
		return new MiPerfilAcademico(Optional.of(plan));
	}
}
