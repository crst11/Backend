package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarRegistroExtendido.Analisis;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaPorImportar;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoPorImportar;
import co.edu.ucundinamarca.cundiapp.domain.model.ResumenOficialDePeriodo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Lo detectado en el reporte, como se le muestra al estudiante antes de guardar nada (SCRUM-23).
 *
 * <p>Viaja todo lo que hace falta para confirmar sin volver a subir el archivo: por eso cada
 * período trae también sus totales oficiales.
 */
public record AnalisisDeImportacionDto(
		String nombreArchivo,
		String programaDelReporte,
		String sedeDelReporte,
		boolean coincideConMiPrograma,
		int detectadas,
		long seVanAGuardar,
		List<PeriodoDto> periodos) {

	public record PeriodoDto(
			String codigo,
			Integer creditosMatriculados,
			Integer creditosAprobados,
			BigDecimal promedioPeriodo,
			BigDecimal promedioAcumulado,
			List<AsignaturaDto> asignaturas) {

		static PeriodoDto desde(PeriodoPorImportar periodo) {
			Optional<ResumenOficialDePeriodo> oficial = Optional.ofNullable(periodo.oficial());
			return new PeriodoDto(
					periodo.codigo(),
					oficial.map(ResumenOficialDePeriodo::creditosMatriculados).orElse(null),
					oficial.map(ResumenOficialDePeriodo::creditosAprobados).orElse(null),
					oficial.map(ResumenOficialDePeriodo::promedioPeriodo).orElse(null),
					oficial.map(ResumenOficialDePeriodo::promedioAcumulado).orElse(null),
					periodo.asignaturas().stream().map(AsignaturaDto::desde).toList());
		}
	}

	/**
	 * `enElPlan` en falso significa que esa asignatura no se va a guardar: sin estar en la ruta de
	 * aprendizaje no hay nombre ni créditos con qué contarla. Se muestra igual, para que el
	 * estudiante sepa que la vimos y no creer que se perdió.
	 */
	public record AsignaturaDto(
			String codigo,
			String nombre,
			int creditos,
			BigDecimal nota,
			String estado,
			boolean enElPlan,
			boolean yaEstaba) {

		static AsignaturaDto desde(AsignaturaPorImportar asignatura) {
			return new AsignaturaDto(
					asignatura.codigo(),
					asignatura.nombre(),
					asignatura.creditos(),
					asignatura.notaDefinitiva(),
					asignatura.estado().valorEnBd(),
					asignatura.enElPlan(),
					asignatura.yaEstaba());
		}
	}

	public static AnalisisDeImportacionDto desde(Analisis analisis, String nombreArchivo) {
		return new AnalisisDeImportacionDto(
				nombreArchivo,
				analisis.programaDelReporte(),
				analisis.sedeDelReporte(),
				analisis.coincideConMiPrograma(),
				analisis.detectadas(),
				analisis.seVanAGuardar(),
				analisis.periodos().stream().map(PeriodoDto::desde).toList());
	}
}
