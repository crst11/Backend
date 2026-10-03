package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.MiHistorial;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaCursada;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoCursado;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * El historial como lo lee el estudiante: períodos, notas, promedios y avance (SCRUM-22).
 *
 * <p>Cuando el reporte oficial trae el promedio, es ese el que viaja, y `fuente` lo dice. El motor
 * propio solo cubre lo que la universidad todavía no ha publicado, como el período en curso.
 * Academusoft imprime las notas con un decimal pero las guarda con dos, así que nuestro cálculo
 * puede diferir en centésimas; contradecirle a la universidad su propia nota sería un error de la
 * app, no suyo.
 */
public record MiHistorialDto(
		List<PeriodoDto> periodos,
		BigDecimal promedioAcumulado,
		String fuenteDelAcumulado,
		int creditosAprobados,
		Integer creditosDelPrograma,
		BigDecimal porcentajeDeAvance) {

	private static final String OFICIAL = "oficial";
	private static final String CALCULADO = "calculado";

	public record PeriodoDto(
			String codigo,
			int creditosMatriculados,
			int creditosAprobados,
			BigDecimal promedio,
			String fuenteDelPromedio,
			List<AsignaturaCursadaDto> asignaturas) {
	}

	public record AsignaturaCursadaDto(
			String codigo, String nombre, int creditos, BigDecimal nota, String estado, boolean pondera) {

		static AsignaturaCursadaDto desde(AsignaturaCursada asignatura) {
			return new AsignaturaCursadaDto(
					asignatura.codigo(),
					asignatura.nombre(),
					asignatura.creditos(),
					asignatura.notaDefinitiva(),
					asignatura.estado().valorEnBd(),
					asignatura.ponderaEnElPromedio());
		}
	}

	public static MiHistorialDto desde(MiHistorial historial) {
		List<PeriodoDto> periodos = historial.historial().periodos().stream()
				.map(periodo -> periodoDto(historial, periodo))
				.toList();

		Optional<BigDecimal> acumuladoOficial = ultimoAcumuladoOficial(historial);
		Optional<BigDecimal> acumulado = acumuladoOficial.or(() -> historial.historial().promedioAcumulado());

		Integer creditosDelPrograma = historial.programaElegido()
				.map(programa -> programa.totalCreditos())
				.orElse(null);

		return new MiHistorialDto(
				periodos,
				acumulado.orElse(null),
				fuente(acumuladoOficial.isPresent(), acumulado.isPresent()),
				historial.historial().creditosAprobados(),
				creditosDelPrograma,
				creditosDelPrograma == null ? null : historial.historial().porcentajeDeAvance(creditosDelPrograma));
	}

	private static PeriodoDto periodoDto(MiHistorial historial, PeriodoCursado periodo) {
		Optional<BigDecimal> oficial = historial.oficialDe(periodo.codigo())
				.flatMap(resumen -> resumen.promedioDelPeriodo());
		Optional<BigDecimal> promedio = oficial.or(periodo::promedio);

		return new PeriodoDto(
				periodo.codigo(),
				periodo.creditosMatriculados(),
				periodo.creditosAprobados(),
				promedio.orElse(null),
				fuente(oficial.isPresent(), promedio.isPresent()),
				periodo.asignaturas().stream().map(AsignaturaCursadaDto::desde).toList());
	}

	/**
	 * El acumulado oficial solo sirve si el reporte cubre hasta el último período cursado. Si el
	 * estudiante ya cursó algo después de su última importación, ese número quedó viejo: mostrarlo
	 * le diría que su promedio es el de hace un semestre.
	 */
	private static Optional<BigDecimal> ultimoAcumuladoOficial(MiHistorial historial) {
		Optional<String> ultimoCursado = historial.historial().periodos().stream()
				.map(PeriodoCursado::codigo)
				.reduce((primero, siguiente) -> siguiente);
		if (ultimoCursado.isEmpty()) {
			return Optional.empty();
		}
		return historial.oficialDe(ultimoCursado.get()).flatMap(resumen -> resumen.acumulado());
	}

	private static String fuente(boolean esOficial, boolean hayValor) {
		if (!hayValor) {
			return null;
		}
		return esOficial ? OFICIAL : CALCULADO;
	}
}
