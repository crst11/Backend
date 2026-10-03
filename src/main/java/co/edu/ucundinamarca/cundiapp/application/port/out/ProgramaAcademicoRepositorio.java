package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import java.util.List;
import java.util.Optional;

/** El catálogo de programas y sus rutas de aprendizaje (RF02). */
public interface ProgramaAcademicoRepositorio {

	/** Todos los programas con plan vigente, para que el estudiante elija el suyo. */
	List<ProgramaAcademico> listarProgramas();

	/** El plan vigente de un programa, con sus asignaturas y prerrequisitos. */
	Optional<PlanDeEstudios> buscarPlanVigenteDe(String codigoPrograma);

	Optional<PlanDeEstudios> buscarPlanPorCodigo(String codigoPlan);
}
