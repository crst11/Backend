package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDePeticionesPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadasPeticionesException;
import java.time.Duration;

/**
 * Cuántos PDF puede analizar un estudiante seguidos (SCRUM-88). Leer un PDF es lo más caro que hace el
 * servidor, y un estudiante real sube uno o dos. El cupo es **por estudiante** y se comparte entre el
 * Registro Académico Extendido y el Reporte de Horario: en la universidad muchos comparten la misma IP,
 * así que limitar por IP dejaría a unos sin servicio por culpa de otros.
 */
final class LimiteDeAnalisisDePdf {

	static final int MAXIMO = 10;
	static final Duration VENTANA = Duration.ofMinutes(10);

	private LimiteDeAnalisisDePdf() {
	}

	static void exigirCupo(LimitadorDePeticionesPort limitador, int idEstudiante) {
		limitador.esperaSiSeExcede("analisis-pdf:" + idEstudiante, MAXIMO, VENTANA).ifPresent(espera -> {
			throw new DemasiadasPeticionesException(espera);
		});
	}
}
