package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import java.util.List;

/** Caso de uso: ver los programas entre los que el estudiante puede elegir (RF02). */
public interface ListarProgramas {

	List<ProgramaAcademico> ejecutar();
}
