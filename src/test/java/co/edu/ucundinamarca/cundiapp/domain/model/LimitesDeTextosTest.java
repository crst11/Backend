package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Cuantos caracteres admite cada texto del dominio, por los dos lados del limite (SCRUM-242, pruebas de mutacion).
 *
 * <p>Los limites son los de las columnas de la base de datos: uno de mas y el guardado falla con un error de PostgreSQL en vez
 * de un mensaje que la persona entienda. Por eso el dominio los comprueba y por eso se prueba el valor exacto: un {@code >}
 * convertido en {@code >=} rechazaria un nombre que si cabe, y uno convertido en {@code <} dejaria pasar uno que no.
 */
class LimitesDeTextosTest {

	private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

	private static void admite(Executable construir) {
		assertThatCode(construir::execute).doesNotThrowAnyException();
	}

	private static void rechaza(Executable construir) {
		assertThatThrownBy(construir::execute).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	private static String de(int largo) {
		return "a".repeat(largo);
	}

	private static BloqueDeOferta bloque(int horaDeInicio) {
		return new BloqueDeOferta(DiaDeLaSemana.LUNES, LocalTime.of(horaDeInicio, 0), LocalTime.of(horaDeInicio + 1, 0));
	}

	@Test
	void elTituloDelAvisoAdmiteHasta120Caracteres() {
		admite(() -> Aviso.nuevo(1, TipoDeAviso.VENCIMIENTO_PROXIMO, de(120), "mensaje", AHORA));
		rechaza(() -> Aviso.nuevo(1, TipoDeAviso.VENCIMIENTO_PROXIMO, de(121), "mensaje", AHORA));
	}

	@Test
	void losIntentosDeEntregaVanDeCeroAlMaximoAmbosIncluidos() {
		admite(() -> avisoConIntentos(0));
		admite(() -> avisoConIntentos(Aviso.MAXIMO_DE_INTENTOS));
		rechaza(() -> avisoConIntentos(-1));
		rechaza(() -> avisoConIntentos(Aviso.MAXIMO_DE_INTENTOS + 1));
	}

	private static Aviso avisoConIntentos(int intentos) {
		return new Aviso(null, 1, TipoDeAviso.VENCIMIENTO_PROXIMO, "titulo", "mensaje", AHORA, EstadoDeEnvio.PENDIENTE, false,
				intentos);
	}

	@Test
	void losNombresDelDocenteAdmitenHasta80CaracteresCadaUno() {
		admite(() -> new Docente(de(Docente.LARGO_MAXIMO), "Perez"));
		admite(() -> new Docente("Ana", de(Docente.LARGO_MAXIMO)));
		rechaza(() -> new Docente(de(Docente.LARGO_MAXIMO + 1), "Perez"));
		rechaza(() -> new Docente("Ana", de(Docente.LARGO_MAXIMO + 1)));
	}

	@Test
	void elEspacioFisicoRespetaElLargoDeSusTresColumnas() {
		admite(() -> new EspacioFisico(de(EspacioFisico.LARGO_DEL_SALON), null, "Fusagasuga"));
		rechaza(() -> new EspacioFisico(de(EspacioFisico.LARGO_DEL_SALON + 1), null, "Fusagasuga"));

		admite(() -> new EspacioFisico("101", de(EspacioFisico.LARGO_DEL_BLOQUE), "Fusagasuga"));
		rechaza(() -> new EspacioFisico("101", de(EspacioFisico.LARGO_DEL_BLOQUE + 1), "Fusagasuga"));

		admite(() -> new EspacioFisico("101", null, de(EspacioFisico.LARGO_DE_LA_SEDE)));
		rechaza(() -> new EspacioFisico("101", null, de(EspacioFisico.LARGO_DE_LA_SEDE + 1)));
	}

	@Test
	void elCodigoDelGrupoAdmiteHasta30Caracteres() {
		admite(() -> new GrupoOfertado(de(GrupoOfertado.MAXIMO_DEL_CODIGO), List.of(bloque(7))));
		rechaza(() -> new GrupoOfertado(de(GrupoOfertado.MAXIMO_DEL_CODIGO + 1), List.of(bloque(7))));
	}

	@Test
	void unGrupoAdmiteHastaOchoBloquesQueNoSeSolapen() {
		admite(() -> new GrupoOfertado("A", bloquesSeguidos(GrupoOfertado.MAXIMO_DE_BLOQUES)));
		rechaza(() -> new GrupoOfertado("A", bloquesSeguidos(GrupoOfertado.MAXIMO_DE_BLOQUES + 1)));
	}

	/** Bloques de una hora, uno tras otro desde las 6: terminar a la hora en que empieza el siguiente no es solaparse. */
	private static List<BloqueDeOferta> bloquesSeguidos(int cantidad) {
		List<BloqueDeOferta> bloques = new ArrayList<>();
		for (int i = 0; i < cantidad; i++) {
			bloques.add(bloque(6 + i));
		}
		return bloques;
	}

	@Test
	void elNombreDeLaAsignaturaOfertadaAdmiteHasta80Caracteres() {
		var grupo = new GrupoOfertado("A", List.of(bloque(7)));
		admite(() -> new AsignaturaOfertada(de(AsignaturaOfertada.MAXIMO_DEL_NOMBRE), List.of(grupo)));
		rechaza(() -> new AsignaturaOfertada(de(AsignaturaOfertada.MAXIMO_DEL_NOMBRE + 1), List.of(grupo)));
	}

	@Test
	void elGrupoDeLaAsignaturaConHorarioAdmiteHasta20Caracteres() {
		admite(() -> new AsignaturaConHorario("ING101", de(AsignaturaConHorario.LARGO_DEL_GRUPO), List.of()));
		rechaza(() -> new AsignaturaConHorario("ING101", de(AsignaturaConHorario.LARGO_DEL_GRUPO + 1), List.of()));
	}

	@Test
	void laSedePreferidaAdmiteHasta60Caracteres() {
		admite(() -> new PreferenciaDeHorario(VistaDelHorario.DIA, de(PreferenciaDeHorario.LARGO_DE_LA_SEDE), OrdenDelHorario.HORA));
		rechaza(() -> new PreferenciaDeHorario(VistaDelHorario.DIA, de(PreferenciaDeHorario.LARGO_DE_LA_SEDE + 1),
				OrdenDelHorario.HORA));
	}

	@Test
	void elPendienteRespetaElLargoDeSuTituloYDeSuDescripcion() {
		admite(() -> pendiente(de(Pendiente.LARGO_DEL_TITULO), "detalle"));
		rechaza(() -> pendiente(de(Pendiente.LARGO_DEL_TITULO + 1), "detalle"));
		admite(() -> pendiente("Taller", de(Pendiente.LARGO_DE_LA_DESCRIPCION)));
		rechaza(() -> pendiente("Taller", de(Pendiente.LARGO_DE_LA_DESCRIPCION + 1)));
	}

	private static Pendiente pendiente(String titulo, String descripcion) {
		return Pendiente.nuevo(1, null, titulo, descripcion, AHORA.plusSeconds(3600), PrioridadDePendiente.MEDIA, AHORA);
	}

	@Test
	void laDireccionDeLaSuscripcionAdmiteHasta500Caracteres() {
		var claves = ClavesDePushDePrueba.nueva();
		String base = "https://fcm.googleapis.com/fcm/send/";
		admite(() -> new SuscripcionPush(base + de(500 - base.length()), claves.clavePublica(), claves.secretoEnBase64()));
		rechaza(() -> new SuscripcionPush(base + de(501 - base.length()), claves.clavePublica(), claves.secretoEnBase64()));
	}
}
