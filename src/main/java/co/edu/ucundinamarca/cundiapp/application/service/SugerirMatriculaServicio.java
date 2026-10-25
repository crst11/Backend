package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.SugerenciaParaMatricular;
import co.edu.ucundinamarca.cundiapp.application.port.in.SugerenciaParaMatricular.Razon;
import co.edu.ucundinamarca.cundiapp.application.port.in.SugerirMatricula;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HistorialAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.HistorialAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.SugerenciaDeMatricula;
import co.edu.ucundinamarca.cundiapp.domain.service.SugeridorDeMatricula;
import java.util.Optional;

/**
 * Lee el plan que el estudiante eligió y lo que lleva cursado, y le pide al motor la sugerencia (RF13, SCRUM-244).
 * Solo lee: no guarda nada. El estudiante sale del token, nunca de un parámetro, así que nadie ve el plan de otro.
 */
public class SugerirMatriculaServicio implements SugerirMatricula {

	private final EstudianteRepositorio estudiantes;
	private final ProgramaAcademicoRepositorio programas;
	private final HistorialAcademicoRepositorio historiales;
	private final SugeridorDeMatricula sugeridor;

	public SugerirMatriculaServicio(
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas,
			HistorialAcademicoRepositorio historiales,
			SugeridorDeMatricula sugeridor) {
		this.estudiantes = estudiantes;
		this.programas = programas;
		this.historiales = historiales;
		this.sugeridor = sugeridor;
	}

	@Override
	public SugerenciaParaMatricular ejecutar(int idEstudiante) {
		Optional<PlanDeEstudios> plan = estudiantes.planDe(idEstudiante).flatMap(programas::buscarPlanPorCodigo);
		if (plan.isEmpty()) {
			return new SugerenciaParaMatricular(null, Razon.SIN_PLAN);
		}
		HistorialAcademico historial = historiales.historialDe(idEstudiante);
		if (historial.periodos().isEmpty()) {
			return new SugerenciaParaMatricular(null, Razon.SIN_HISTORIAL);
		}
		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan.get(), historial);
		Razon razon = sugerencia.semestreSugerido() == null ? Razon.NADA_POR_MATRICULAR : null;
		return new SugerenciaParaMatricular(sugerencia, razon);
	}
}
