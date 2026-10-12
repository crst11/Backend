package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NotaAcumulada;
import java.math.BigDecimal;

/**
 * Anotar la nota de una actividad (RF05, SCRUM-28).
 *
 * <p>Guarda o corrige: registrar de nuevo la misma actividad reemplaza la nota, así que la pantalla
 * no necesita distinguir entre "poner" y "cambiar". Devuelve lo que el estudiante lleva ya con esa
 * nota, que es lo que quiere ver apenas la escribe.
 */
public interface RegistrarMiCalificacion {

	Resultado ejecutar(int idEstudiante, int idMatricula, int consecCategoria, int consecActividad, BigDecimal nota);

	record Resultado(Nota nota, NotaAcumulada acumulado) {
	}
}
