package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Un período académico identificado como lo nombra la universidad: "2024-2" (RF02, RF03).
 *
 * <p><b>Sobre las fechas.</b> El Registro Académico Extendido dice qué períodos cursó el
 * estudiante, pero no cuándo empezaron ni cuándo terminaron, y el esquema las exige. Se derivan
 * de la convención de la universidad (el primer semestre va de febrero a junio y el segundo de
 * agosto a diciembre) y son aproximadas a propósito: sirven para ordenar el historial, que es
 * para lo único que se usan aquí. Cuando el calendario académico se importe de verdad, estas
 * fechas se reemplazan por las oficiales.
 */
public record PeriodoAcademico(int anio, int semestre) {

	private static final Pattern CODIGO = Pattern.compile("^(\\d{4})-([12])$");

	public PeriodoAcademico {
		if (semestre != 1 && semestre != 2) {
			throw new ReglaDeNegocioVioladaException("Un período solo puede ser el semestre 1 o el 2");
		}
	}

	public static PeriodoAcademico desdeCodigo(String codigo) {
		var encontrado = CODIGO.matcher(codigo == null ? "" : codigo.trim());
		if (!encontrado.matches()) {
			throw new ReglaDeNegocioVioladaException(
					"El período debe venir como año-semestre, por ejemplo 2024-2");
		}
		return new PeriodoAcademico(
				Integer.parseInt(encontrado.group(1)), Integer.parseInt(encontrado.group(2)));
	}

	public String codigo() {
		return anio + "-" + semestre;
	}

	/** Aproximada: ver la nota de la clase. */
	public LocalDate inicioAproximado() {
		return semestre == 1 ? LocalDate.of(anio, 2, 1) : LocalDate.of(anio, 8, 1);
	}

	/** Aproximada: ver la nota de la clase. */
	public LocalDate finAproximado() {
		return semestre == 1 ? LocalDate.of(anio, 6, 30) : LocalDate.of(anio, 12, 15);
	}

	public boolean terminoAntesDe(LocalDate fecha) {
		return finAproximado().isBefore(fecha);
	}
}
