package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import java.time.Instant;
import java.util.Optional;

/**
 * El último nivel de riesgo que se conoció de cada asignatura (RF10, SCRUM-50). El riesgo en sí no se guarda:
 * esto es solo el punto de comparación para saber si cambió.
 */
public interface RiesgoAvisadoRepositorio {

	/** El último nivel conocido, o vacío si nunca se calculó el riesgo de esa matrícula. */
	Optional<NivelDeRiesgo> nivelDe(int idMatricula);

	/** Deja el nivel como el último conocido; si la matrícula ya tenía uno, lo reemplaza. */
	void guardar(int idMatricula, NivelDeRiesgo nivel, Instant ahora);
}
