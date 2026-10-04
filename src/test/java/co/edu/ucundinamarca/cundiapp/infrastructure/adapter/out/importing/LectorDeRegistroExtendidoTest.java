package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaDetectada;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoDetectado;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Lee el texto de un Registro Académico Extendido de Academusoft (SCRUM-23).
 *
 * <p><b>Qué se lee y qué no.</b> El lector saca el código de cada asignatura, su nota definitiva y
 * los totales que el reporte publica por período. <b>No lee los nombres</b>: en el PDF las columnas
 * se entreveran al pasar a texto (el nombre de una asignatura se parte en varios renglones y se
 * mezcla con el grupo), y además no hacen falta, porque los nombres oficiales ya están en la base
 * con la ruta de aprendizaje. Sacarlos del PDF sería trabajo frágil para un dato que ya se tiene.
 *
 * <p>El texto de ejemplo copia la estructura del reporte real, con sus tropiezos: renglones
 * partidos, decimales con coma, las columnas Final, Hab. y Def., y el prefijo DN- de las de
 * diagnóstico y nivelatorio. Los datos personales están cambiados: ningún reporte de nadie entra
 * al repositorio, y el documento de identidad no se lee nunca (Ley 1581 de 2012).
 */
class LectorDeRegistroExtendidoTest {

	private static final String REPORTE = """
			Fecha del Reporte 07-09-2026 16:38:48 Académico - Academusoft 4.0
			Consultar Registro Académico Extendido
			Información
			Identificación Nombre
			1111111111 NOMBRE DE PRUEBA APELLIDO DE PRUEBA
			Categoría Situación Total de Créditos Cursados Total de Créditos Aprobados Promedio Acumulado
			ANTIGUO ACTIVO 34 31 4.6
			Programa Jornada Ruta de Aprendizaje Ubicación Semestral
			INGENIERIA DE SISTEMAS Y COMPUTACION 2020 - MIXTA INGENIERIA DE SISTEMAS Y 5
			FUSAGASUGÁ COMPUTACION 2020 FUSAGASUGA
			Período Período Ponderación Ponderación Aprobada Promedio Período Promedio Acumulado
			Institucional Matriculada
			2024 - 2 16 16 4.4 4.4
			Código Campo de aprendizaje Tipo Ponderación Grupo Final Hab. Def.
			CAD612021101 ÁLGEBRA LINEAL NORMAL 3 F.103M 4,2 4,2
			DN-CAI1002020202 DIAGNOSTICO Y NIVELATORIO COMUNICACIÓN Y LECTURA NORMAL 0 DN- 4,0 4,0
			CRÍTICA I COMUNICACIÓN
			Y LECTURA
			CAD612021106 FUNDAMENTOS DE ELECTRÓNICA NORMAL 4 F.104M 4,3 4,3
			CAD612021103 FUNDAMENTOS DE INGENIERÍA NORMAL 2 F.102M 4,3 4,3
			CAD612021105 MATEMÁTICAS DISCRETAS NORMAL 2 F.102M 4,6 4,6
			CAD612021102 PENSAMIENTO ALGORÍTMICO NORMAL 3 F.102M 4,7 4,7
			CAD612021104 PENSAMIENTO SISTÉMICO Y AUTOMATIZACIÓN NORMAL 2 F.101M 4,8 4,8
			Período Período Ponderación Ponderación Aprobada Promedio Período Promedio Acumulado
			Institucional Matriculada
			2025 - 1 18 15 4.4 4.4
			Código Campo de aprendizaje Tipo Ponderación Grupo Final Hab. Def.
			CAD612021207 CALCULO DIFERENCIAL NORMAL 4 SIS.FUSA 5,0 5,0
			201M
			CAI1002020202 COMUNICACIÓN Y LECTURA CRÍTICA I NORMAL 2 CLC1-M3 4,6 4,6
			CAD612021210 ESTADÍSTICA,PROBABILIDAD E INFERENCIA NORMAL 3 SIS.FUSA 5,0 5,0
			201M
			CAD612021208 FISICA I NORMAL 4 SIS.FUSA 4,9 4,9
			201M
			CAD612021209 PROGRAMACIÓN I NORMAL 3 SIS.FUSA 2,0 2,5 2,5
			201M
			CAI1002020201 RAZONAMIENTO LÓGICO Y CUANTITATIVO NORMAL 2 RLC-M4 4,9 4,9
			Pág 1 de 1.
			""";

	private final LectorDeRegistroExtendido lector = new LectorDeRegistroExtendido();

	private PeriodoDetectado periodo(String codigo) {
		return lector.leer(REPORTE).periodos().stream()
				.filter(p -> p.codigo().equals(codigo))
				.findFirst()
				.orElseThrow();
	}

	private AsignaturaDetectada asignatura(String periodo, String codigo) {
		return periodo(periodo).asignaturas().stream()
				.filter(a -> a.codigo().equals(codigo))
				.findFirst()
				.orElseThrow();
	}

	@Test
	void reconoceElProgramaYLaSede() {
		var reporte = lector.leer(REPORTE);

		assertThat(reporte.programa()).isEqualTo("INGENIERIA DE SISTEMAS Y COMPUTACION");
		assertThat(reporte.sede()).isEqualTo("FUSAGASUGÁ");
	}

	@Test
	void detectaLosPeriodosEnOrden() {
		assertThat(lector.leer(REPORTE).periodos()).extracting(PeriodoDetectado::codigo)
				.containsExactly("2024-2", "2025-1");
	}

	@Test
	void detectaTodasLasAsignaturasDeCadaPeriodo() {
		assertThat(periodo("2024-2").asignaturas()).hasSize(7);
		assertThat(periodo("2025-1").asignaturas()).hasSize(6);
	}

	@Test
	void leeElCodigoYLaNotaDefinitivaDeCadaAsignatura() {
		var algebra = asignatura("2024-2", "CAD612021101");

		assertThat(algebra.codigo()).isEqualTo("CAD612021101");
		assertThat(algebra.notaDefinitiva()).isEqualByComparingTo(new BigDecimal("4.2"));
	}

	@Test
	void noSeTragaLosRenglonesSueltosComoSiFueranAsignaturas() {
		// "201M" continúa el grupo SIS.FUSA y "Y LECTURA" continúa un nombre: ninguno es una asignatura.
		var codigos = lector.leer(REPORTE).periodos().stream()
				.flatMap(p -> p.asignaturas().stream())
				.map(AsignaturaDetectada::codigo)
				.toList();

		assertThat(codigos).doesNotContain("201M", "Y", "CRÍTICA");
		assertThat(codigos).allMatch(codigo -> codigo.length() >= 12);
	}

	@Test
	void cuandoHayHabilitacionManda_laDefinitivaYNoLaFinal() {
		// Programación I: perdió con 2,0, habilitó con 2,5 y quedó en 2,5. La definitiva es la última.
		assertThat(asignatura("2025-1", "CAD612021209").notaDefinitiva())
				.isEqualByComparingTo(new BigDecimal("2.5"));
	}

	@Test
	void losDecimalesConComaSeLeenComoNumeros() {
		assertThat(asignatura("2024-2", "CAD612021104").notaDefinitiva())
				.isEqualByComparingTo(new BigDecimal("4.8"));
	}

	@Test
	void marcaAprobadaOReprobadaSegunLaNotaMinima() {
		assertThat(asignatura("2024-2", "CAD612021101").aprobada()).isTrue();
		// 2,5 no alcanza el 3,0 del Reglamento Estudiantil.
		assertThat(asignatura("2025-1", "CAD612021209").aprobada()).isFalse();
	}

	@Test
	void traeLosTotalesOficialesDeCadaPeriodo() {
		var oficiales = lector.leer(REPORTE).oficiales();

		assertThat(oficiales).extracting(r -> r.codigoPeriodo()).containsExactly("2024-2", "2025-1");
		assertThat(oficiales.getFirst().creditosMatriculados()).isEqualTo(16);
		assertThat(oficiales.getFirst().creditosAprobados()).isEqualTo(16);
		assertThat(oficiales.getFirst().promedioPeriodo()).isEqualByComparingTo(new BigDecimal("4.4"));
		assertThat(oficiales.getFirst().promedioAcumulado()).isEqualByComparingTo(new BigDecimal("4.4"));
	}

	@Test
	void nuncaDevuelveElDocumentoDeIdentidad() {
		// Ley 1581: el documento del estudiante no se almacena, así que ni siquiera sale del lector.
		assertThat(lector.leer(REPORTE).toString()).doesNotContain("1111111111");
	}

	@Test
	void unTextoQueNoEsEsteReporteSeRechaza() {
		assertThatThrownBy(() -> lector.leer("Consultar Notas Actuales\nCAD612021523 BASE DE DATOS 2 - 0.0"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Registro Académico Extendido");
	}

	@Test
	void unReporteSinNingunPeriodoSeRechaza() {
		assertThatThrownBy(() -> lector.leer("Consultar Registro Académico Extendido\nInformación"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("ningún período");
	}
}
