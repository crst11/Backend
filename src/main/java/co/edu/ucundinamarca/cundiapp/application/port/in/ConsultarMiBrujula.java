package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.RiesgoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import java.util.List;

/**
 * La Brújula Académica: el riesgo de cada asignatura en curso (RF09, SCRUM-35).
 *
 * <p>Va de lo más urgente a lo menos. Las que todavía no tienen estructura de evaluación aparecen
 * al final como «sin datos», para que el estudiante vea que faltan y no que no existen.
 */
public interface ConsultarMiBrujula {

	Resultado ejecutar(int idEstudiante);

	record Resultado(Meta meta, UmbralesDeRiesgo umbrales, List<ItemDeBrujula> asignaturas) {
	}

	record ItemDeBrujula(AsignaturaMatriculada asignatura, RiesgoDeAsignatura riesgo) {
	}
}
