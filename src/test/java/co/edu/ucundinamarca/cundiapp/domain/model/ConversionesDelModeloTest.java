package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Lo que se guarda en la base de datos y lo que se muestra se convierten de ida y vuelta, y un valor que no existe se rechaza
 * (SCRUM-242, pruebas de mutacion). Estas conversiones solo las ejercitaban las pruebas de los adaptadores de persistencia, no las del
 * dominio: PIT las marcaba sin cobertura aunque un error ahi guardaria o leeria un estado equivocado sin que nada fallara.
 */
class ConversionesDelModeloTest {

	/** Cada valor del enum sobrevive a convertirse en texto y volver, y un texto desconocido no se traga en silencio. */
	private static <E extends Enum<E>> void idaYVuelta(E[] valores, Function<E, String> aTexto, Function<String, E> aEnum) {
		sobreviveAlViaje(valores, aTexto, aEnum);
		assertThatThrownBy(() -> aEnum.apply("no_existe")).isInstanceOf(RuntimeException.class);
		assertThatThrownBy(() -> aEnum.apply(null)).isInstanceOf(RuntimeException.class);
	}

	private static <E extends Enum<E>> void sobreviveAlViaje(E[] valores, Function<E, String> aTexto, Function<String, E> aEnum) {
		for (E valor : valores) {
			String texto = aTexto.apply(valor);
			assertThat(texto).as("texto de %s", valor).isNotBlank();
			assertThat(aEnum.apply(texto)).as("vuelta de %s", texto).isSameAs(valor);
		}
		assertThat(Arrays.stream(valores).map(aTexto).distinct()).as("textos distintos").hasSize(valores.length);
	}

	@Test
	void losEstadosYTiposDeLaBaseDeDatosVanYVuelven() {
		idaYVuelta(EstadoDeAsignatura.values(), EstadoDeAsignatura::valorEnBd, EstadoDeAsignatura::desdeBd);
		idaYVuelta(EstadoDeEntrega.values(), EstadoDeEntrega::valorEnBd, EstadoDeEntrega::desdeBd);
		idaYVuelta(EstadoDeImportacion.values(), EstadoDeImportacion::valorEnBd, EstadoDeImportacion::desdeBd);
		// Un estado de pendiente que no se reconoce se lee como abierto: es lo seguro, porque asi nunca se da por cumplido algo que no lo esta.
		sobreviveAlViaje(EstadoDePendiente.values(), EstadoDePendiente::valorEnBd, EstadoDePendiente::desdeBd);
		assertThat(EstadoDePendiente.desdeBd("no_existe")).isSameAs(EstadoDePendiente.ABIERTO);
		assertThat(EstadoDePendiente.desdeBd(null)).isSameAs(EstadoDePendiente.ABIERTO);
		idaYVuelta(OrigenDeCategoria.values(), OrigenDeCategoria::valorEnBd, OrigenDeCategoria::desdeBd);
		idaYVuelta(PrioridadDePendiente.values(), PrioridadDePendiente::valorEnBd, PrioridadDePendiente::desdeBd);
		idaYVuelta(PropositoDelCodigo.values(), PropositoDelCodigo::valorEnBaseDeDatos, PropositoDelCodigo::desdeBaseDeDatos);
		idaYVuelta(TipoDeActividad.values(), TipoDeActividad::valorEnBd, TipoDeActividad::desdeBd);
		idaYVuelta(TipoDeAsignatura.values(), TipoDeAsignatura::valorEnBd, TipoDeAsignatura::desdeBd);
		idaYVuelta(OrdenDelHorario.values(), OrdenDelHorario::valorEnBd, OrdenDelHorario::desdeBd);
		idaYVuelta(VistaDelHorario.values(), VistaDelHorario::valorEnBd, VistaDelHorario::desdeBd);
		idaYVuelta(FranjaDelDia.values(), FranjaDelDia::valorApi, FranjaDelDia::desdeApi);
	}

	@Test
	void losEstadosQueSoloSeMuestranTienenTextoPropioYDistinto() {
		assertThat(Arrays.stream(EstadoDeLaClase.values()).map(EstadoDeLaClase::valor)).doesNotContain("").doesNotHaveDuplicates();
		assertThat(Arrays.stream(TipoDeDia.values()).map(TipoDeDia::valor)).doesNotContain("").doesNotHaveDuplicates();
		assertThat(Arrays.stream(TipoDePaso.values()).map(TipoDePaso::valor)).doesNotContain("").doesNotHaveDuplicates();
		assertThat(EstadoDeLaClase.EN_CURSO.valor()).isEqualTo("en_curso");
		assertThat(TipoDeDia.FUERA_DE_PERIODO.valor()).isEqualTo("fuera_de_periodo");
		assertThat(TipoDePaso.REGISTRO_EXTENDIDO.valor()).isEqualTo("registro_extendido");
	}

	@Test
	void elPeriodoSeLeeComoAnioGuionSemestre() {
		var periodo = PeriodoAcademico.desdeCodigo(" 2026-2 ");

		assertThat(periodo.anio()).isEqualTo(2026);
		assertThat(periodo.semestre()).isEqualTo(2);
		assertThat(PeriodoAcademico.desdeCodigo("2026-1").semestre()).isEqualTo(1);
		assertThatThrownBy(() -> PeriodoAcademico.desdeCodigo("2026-3")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> PeriodoAcademico.desdeCodigo("2026-0")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> PeriodoAcademico.desdeCodigo("2026")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> PeriodoAcademico.desdeCodigo(null)).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void laAsignaturaMatriculadaTieneEstructuraDesdeLaPrimeraCategoria() {
		var sin = new AsignaturaMatriculada(1, "ING101", "Calculo", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 0, 0);
		var con = new AsignaturaMatriculada(1, "ING101", "Calculo", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 1, 0);

		assertThat(sin.tieneEstructura()).isFalse();
		assertThat(con.tieneEstructura()).isTrue();
		assertThat(con.estaEnCurso()).isTrue();
	}

	@Test
	void laNotaDeUnaAsignaturaCursadaSeEntregaComoOpcional() {
		assertThat(new AsignaturaCursada("A", "A", 3, new BigDecimal("4.2"), EstadoDeAsignatura.APROBADA).nota())
				.contains(new BigDecimal("4.2"));
		assertThat(new AsignaturaCursada("A", "A", 3, null, EstadoDeAsignatura.EN_CURSO).nota()).isEmpty();
	}

	@Test
	void soloLaCuentaActivaEstaActiva() {
		var correo = new CorreoInstitucional("ana.perez@ucundinamarca.edu.co");
		var pendiente = new Estudiante(1, "Ana", "Diaz", correo, EstadoCuenta.PENDIENTE, true, Instant.now());

		assertThat(pendiente.estaActiva()).isFalse();
		assertThat(pendiente.activar().estaActiva()).isTrue();
	}

	@Test
	void laEstructuraEncuentraUnaActividadPorLaClaveDeSuCategoriaYSuOrden() {
		var parcial = new ActividadEvaluativa(1, "Parcial", new BigDecimal("60"), null, TipoDeActividad.PARCIAL,
				EstadoDeEntrega.SIN_CALIFICAR);
		var taller = new ActividadEvaluativa(2, "Taller", new BigDecimal("40"), null, TipoDeActividad.TALLER,
				EstadoDeEntrega.SIN_CALIFICAR);
		var corte = new CategoriaDeEvaluacion(1, "Primer corte", new BigDecimal("100"), OrigenDeCategoria.ESTUDIANTE,
				List.of(parcial, taller));
		var estructura = new EstructuraDeEvaluacion(7, List.of(corte));

		assertThat(estructura.actividadDe(1, 2)).contains(taller);
		assertThat(estructura.actividadDe(1, 3)).isEmpty();
		assertThat(estructura.actividadDe(2, 1)).isEmpty();
		assertThat(corte.estaDesglosada()).isTrue();
		assertThat(new CategoriaDeEvaluacion(1, "Vacio", new BigDecimal("100"), OrigenDeCategoria.ESTUDIANTE, List.of())
				.estaDesglosada()).isFalse();
	}
}
