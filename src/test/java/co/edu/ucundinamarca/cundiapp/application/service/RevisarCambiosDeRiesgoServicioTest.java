package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula.ItemDeBrujula;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciasDeAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RiesgoAvisadoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDelRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.RiesgoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAviso;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.service.RedactorDeAvisos;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** El aviso de cambio de riesgo (RF10, SCRUM-50): cuándo se avisa y cuándo solo se anota el nivel. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RevisarCambiosDeRiesgoServicioTest {

	private static final int ESTUDIANTE = 5;
	private static final int CALCULO = 11;
	private static final int REDES = 12;
	private static final Instant AHORA = Instant.parse("2026-10-17T15:00:00Z");

	@Mock
	private ConsultarMiBrujula brujula;

	@Mock
	private PreferenciasDeAvisosRepositorio preferencias;

	@Mock
	private RiesgoAvisadoRepositorio riesgoAvisado;

	@Mock
	private AvisoRepositorio avisos;

	@Mock
	private RelojPort reloj;

	private RevisarCambiosDeRiesgoServicio servicio;

	@BeforeEach
	void armar() {
		given(reloj.ahora()).willReturn(AHORA);
		given(preferencias.de(ESTUDIANTE)).willReturn(PreferenciasDeAvisos.PREDETERMINADAS);
		servicio = new RevisarCambiosDeRiesgoServicio(
				brujula, preferencias, riesgoAvisado, avisos, new RedactorDeAvisos(), reloj);
	}

	private static ItemDeBrujula item(int idMatricula, String nombre, NivelDeRiesgo nivel) {
		var asignatura = new AsignaturaMatriculada(idMatricula, "COD" + idMatricula, nombre, 3, "2026-2",
				EstadoDeAsignatura.EN_CURSO, 3, 5);
		return new ItemDeBrujula(asignatura, new RiesgoDeAsignatura(nivel, MotivoDelRiesgo.DENTRO_DE_LO_ALCANZABLE, null));
	}

	private void laBrujulaDice(ItemDeBrujula... items) {
		given(brujula.ejecutar(ESTUDIANTE))
				.willReturn(new ConsultarMiBrujula.Resultado(Meta.PREDETERMINADA, UmbralesDeRiesgo.PREDETERMINADOS, List.of(items)));
	}

	@Test
	void laPrimeraVezSoloGuardaElNivelSinAvisar() {
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.MEDIO));
		given(riesgoAvisado.nivelDe(CALCULO)).willReturn(Optional.empty());

		servicio.deLaAsignatura(ESTUDIANTE, CALCULO);

		verify(riesgoAvisado).guardar(CALCULO, NivelDeRiesgo.MEDIO, AHORA);
		verify(avisos, never()).crearDeCambioDeRiesgo(any(), anyInt());
	}

	@Test
	void sinCambioNoHaceNada() {
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.MEDIO));
		given(riesgoAvisado.nivelDe(CALCULO)).willReturn(Optional.of(NivelDeRiesgo.MEDIO));

		servicio.deLaAsignatura(ESTUDIANTE, CALCULO);

		verify(riesgoAvisado, never()).guardar(anyInt(), any(), any());
		verify(avisos, never()).crearDeCambioDeRiesgo(any(), anyInt());
	}

	@Test
	void unCambioDeNivelCreaElAvisoYActualizaElNivel() {
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.ALTO));
		given(riesgoAvisado.nivelDe(CALCULO)).willReturn(Optional.of(NivelDeRiesgo.MEDIO));

		servicio.deLaAsignatura(ESTUDIANTE, CALCULO);

		ArgumentCaptor<Aviso> creado = ArgumentCaptor.forClass(Aviso.class);
		verify(avisos).crearDeCambioDeRiesgo(creado.capture(), eq(CALCULO));
		assertThat(creado.getValue().tipo()).isEqualTo(TipoDeAviso.CAMBIO_RIESGO);
		assertThat(creado.getValue().idEstudiante()).isEqualTo(ESTUDIANTE);
		assertThat(creado.getValue().titulo()).isEqualTo("Subió tu riesgo");
		assertThat(creado.getValue().mensaje()).isEqualTo("El riesgo en «Cálculo» pasó de medio a alto.");
		verify(riesgoAvisado).guardar(CALCULO, NivelDeRiesgo.ALTO, AHORA);
	}

	@Test
	void entrarOSalirDeSinDatosActualizaElNivelSinAvisar() {
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.BAJO));
		given(riesgoAvisado.nivelDe(CALCULO)).willReturn(Optional.of(NivelDeRiesgo.SIN_DATOS));

		servicio.deLaAsignatura(ESTUDIANTE, CALCULO);

		verify(avisos, never()).crearDeCambioDeRiesgo(any(), anyInt());
		verify(riesgoAvisado).guardar(CALCULO, NivelDeRiesgo.BAJO, AHORA);
	}

	@Test
	void conElAvisoDeRiesgoApagadoActualizaElNivelSinAvisar() {
		given(preferencias.de(ESTUDIANTE)).willReturn(new PreferenciasDeAvisos(true, true, false, 24));
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.ALTO));
		given(riesgoAvisado.nivelDe(CALCULO)).willReturn(Optional.of(NivelDeRiesgo.BAJO));

		servicio.deLaAsignatura(ESTUDIANTE, CALCULO);

		verify(avisos, never()).crearDeCambioDeRiesgo(any(), anyInt());
		verify(riesgoAvisado).guardar(CALCULO, NivelDeRiesgo.ALTO, AHORA);
	}

	@Test
	void conTodosLosAvisosApagadosTampocoAvisa() {
		given(preferencias.de(ESTUDIANTE)).willReturn(new PreferenciasDeAvisos(false, true, true, 24));
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.ALTO));
		given(riesgoAvisado.nivelDe(CALCULO)).willReturn(Optional.of(NivelDeRiesgo.BAJO));

		servicio.deLaAsignatura(ESTUDIANTE, CALCULO);

		verify(avisos, never()).crearDeCambioDeRiesgo(any(), anyInt());
		verify(riesgoAvisado).guardar(CALCULO, NivelDeRiesgo.ALTO, AHORA);
	}

	@Test
	void revisarUnaAsignaturaNoTocaLasOtras() {
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.ALTO), item(REDES, "Redes", NivelDeRiesgo.ALTO));
		given(riesgoAvisado.nivelDe(CALCULO)).willReturn(Optional.of(NivelDeRiesgo.BAJO));
		given(riesgoAvisado.nivelDe(REDES)).willReturn(Optional.of(NivelDeRiesgo.BAJO));

		servicio.deLaAsignatura(ESTUDIANTE, CALCULO);

		verify(avisos).crearDeCambioDeRiesgo(any(), eq(CALCULO));
		verify(avisos, never()).crearDeCambioDeRiesgo(any(), eq(REDES));
	}

	@Test
	void revisarTodasAvisaSoloDeLasQueCambiaron() {
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.ALTO), item(REDES, "Redes", NivelDeRiesgo.BAJO));
		given(riesgoAvisado.nivelDe(CALCULO)).willReturn(Optional.of(NivelDeRiesgo.BAJO));
		given(riesgoAvisado.nivelDe(REDES)).willReturn(Optional.of(NivelDeRiesgo.BAJO));

		servicio.deTodasLasAsignaturas(ESTUDIANTE);

		verify(avisos).crearDeCambioDeRiesgo(any(), eq(CALCULO));
		verify(avisos, never()).crearDeCambioDeRiesgo(any(), eq(REDES));
	}

	@Test
	void unaMatriculaQueNoEstaEnCursoNoHaceNada() {
		laBrujulaDice(item(CALCULO, "Cálculo", NivelDeRiesgo.ALTO));

		servicio.deLaAsignatura(ESTUDIANTE, 999);

		verify(riesgoAvisado, never()).guardar(anyInt(), any(), any());
		verify(avisos, never()).crearDeCambioDeRiesgo(any(), anyInt());
	}
}
