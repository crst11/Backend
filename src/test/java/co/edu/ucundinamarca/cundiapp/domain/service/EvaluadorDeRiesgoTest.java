package co.edu.ucundinamarca.cundiapp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDelRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.ProyeccionDeNota;
import co.edu.ucundinamarca.cundiapp.domain.model.RiesgoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.SituacionDeLaMeta;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * El riesgo por asignatura (RF09, SCRUM-35): bajo hasta 3.5, medio entre 3.5 y 4.5, alto por encima de
 * 4.5 o cuando la meta ya no se alcanza. Los bordes exactos van al nivel de abajo.
 */
class EvaluadorDeRiesgoTest {

	private final EvaluadorDeRiesgo evaluador = new EvaluadorDeRiesgo();

	private static ProyeccionDeNota alcanzable(String requerida) {
		return new ProyeccionDeNota(Meta.PREDETERMINADA, new BigDecimal("1.00"), new BigDecimal("20.00"),
				new BigDecimal("80.00"), new BigDecimal(requerida), new BigDecimal("5.00"), SituacionDeLaMeta.ALCANZABLE);
	}

	@ParameterizedTest
	@CsvSource({
			"0.00, BAJO",
			"3.00, BAJO",
			"3.50, BAJO",
			"3.51, MEDIO",
			"4.00, MEDIO",
			"4.50, MEDIO",
			"4.51, ALTO",
			"5.00, ALTO"
	})
	void elNivelSaleDeLaNotaQueHaceFaltaConLosUmbralesPorDefecto(String requerida, NivelDeRiesgo esperado) {
		RiesgoDeAsignatura riesgo = evaluador.evaluar(alcanzable(requerida), UmbralesDeRiesgo.PREDETERMINADOS);

		assertThat(riesgo.nivel()).isEqualTo(esperado);
	}

	@Test
	void elMotivoDiceDeQueUmbralPasa() {
		UmbralesDeRiesgo umbrales = UmbralesDeRiesgo.PREDETERMINADOS;

		assertThat(evaluador.evaluar(alcanzable("4.80"), umbrales).motivo()).isEqualTo(MotivoDelRiesgo.SOBRE_EL_UMBRAL_ALTO);
		assertThat(evaluador.evaluar(alcanzable("4.00"), umbrales).motivo()).isEqualTo(MotivoDelRiesgo.SOBRE_EL_UMBRAL_MEDIO);
		assertThat(evaluador.evaluar(alcanzable("2.50"), umbrales).motivo()).isEqualTo(MotivoDelRiesgo.DENTRO_DE_LO_ALCANZABLE);
	}

	@Test
	void cuandoLaMetaYaNoSeAlcanzaElRiesgoEsAltoSinImportarLosUmbrales() {
		ProyeccionDeNota inalcanzable = new ProyeccionDeNota(Meta.PREDETERMINADA, new BigDecimal("1.00"),
				new BigDecimal("80.00"), new BigDecimal("20.00"), null, new BigDecimal("2.00"), SituacionDeLaMeta.INALCANZABLE);

		RiesgoDeAsignatura riesgo = evaluador.evaluar(inalcanzable,
				new UmbralesDeRiesgo(new BigDecimal("4.90"), new BigDecimal("5.00")));

		assertThat(riesgo.nivel()).isEqualTo(NivelDeRiesgo.ALTO);
		assertThat(riesgo.motivo()).isEqualTo(MotivoDelRiesgo.INALCANZABLE);
	}

	@Test
	void conLaMetaYaAlcanzadaElRiesgoEsBajo() {
		ProyeccionDeNota alcanzada = new ProyeccionDeNota(Meta.PREDETERMINADA, new BigDecimal("3.20"),
				new BigDecimal("70.00"), new BigDecimal("30.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("4.70"),
				SituacionDeLaMeta.ALCANZADA);

		RiesgoDeAsignatura riesgo = evaluador.evaluar(alcanzada, UmbralesDeRiesgo.PREDETERMINADOS);

		assertThat(riesgo.nivel()).isEqualTo(NivelDeRiesgo.BAJO);
		assertThat(riesgo.motivo()).isEqualTo(MotivoDelRiesgo.META_ALCANZADA);
	}

	@Test
	void conOtrosUmbralesElMismoNumeroCaeEnOtroNivel() {
		UmbralesDeRiesgo exigentes = new UmbralesDeRiesgo(new BigDecimal("2.50"), new BigDecimal("3.50"));

		assertThat(evaluador.evaluar(alcanzable("3.00"), exigentes).nivel()).isEqualTo(NivelDeRiesgo.MEDIO);
		assertThat(evaluador.evaluar(alcanzable("3.60"), exigentes).nivel()).isEqualTo(NivelDeRiesgo.ALTO);
	}

	@Test
	void sinDatosNoTieneProyeccionNiNotaRequerida() {
		RiesgoDeAsignatura riesgo = RiesgoDeAsignatura.sinDatos();

		assertThat(riesgo.nivel()).isEqualTo(NivelDeRiesgo.SIN_DATOS);
		assertThat(riesgo.motivo()).isEqualTo(MotivoDelRiesgo.SIN_ESTRUCTURA);
		assertThat(riesgo.notaRequerida()).isNull();
	}

	@Test
	void laPrioridadPoneLoMasUrgentePrimeroYLoQueNoTieneDatosAlFinal() {
		UmbralesDeRiesgo umbrales = UmbralesDeRiesgo.PREDETERMINADOS;
		RiesgoDeAsignatura inalcanzable = evaluador.evaluar(new ProyeccionDeNota(Meta.PREDETERMINADA, BigDecimal.ONE,
				BigDecimal.TEN, BigDecimal.TEN, null, BigDecimal.ONE, SituacionDeLaMeta.INALCANZABLE), umbrales);
		RiesgoDeAsignatura alto = evaluador.evaluar(alcanzable("4.90"), umbrales);
		RiesgoDeAsignatura medioDificil = evaluador.evaluar(alcanzable("4.20"), umbrales);
		RiesgoDeAsignatura medioFacil = evaluador.evaluar(alcanzable("3.80"), umbrales);
		RiesgoDeAsignatura bajo = evaluador.evaluar(alcanzable("2.00"), umbrales);

		List<RiesgoDeAsignatura> desordenadas = new ArrayList<>(
				List.of(RiesgoDeAsignatura.sinDatos(), bajo, medioFacil, alto, medioDificil, inalcanzable));
		desordenadas.sort(RiesgoDeAsignatura.PRIORIDAD);

		assertThat(desordenadas).containsExactly(
				inalcanzable, alto, medioDificil, medioFacil, bajo, RiesgoDeAsignatura.sinDatos());
	}
}
