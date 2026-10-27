package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnadirActividadExtra.DatosDeLaActividad;
import co.edu.ucundinamarca.cundiapp.application.port.out.ActividadExtraRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadExtra;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Añadir y quitar actividades extra: lo que los servicios comprueban antes de escribir (SCRUM-266). */
@ExtendWith(MockitoExtension.class)
class ActividadesExtraServicioTest {

	private static final int ESTUDIANTE = 5;

	@Mock
	private ActividadExtraRepositorio actividades;

	private static DatosDeLaActividad datos(String nombre, List<DiaDeLaSemana> dias) {
		return new DatosDeLaActividad(nombre, dias, LocalTime.of(17, 0), LocalTime.of(19, 0), "Coliseo", "Profe Ana");
	}

	@Test
	void guardaUnaActividadPorDiaConElMaximoDelDominio() {
		given(actividades.guardarSiCaben(anyInt(), any(), anyInt())).willAnswer(i -> Optional.of(i.getArgument(1)));

		var guardadas = new AnadirActividadExtraServicio(actividades)
				.ejecutar(ESTUDIANTE, datos("Baile", List.of(DiaDeLaSemana.LUNES, DiaDeLaSemana.MIERCOLES)));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<ActividadExtra>> captura = ArgumentCaptor.forClass(List.class);
		verify(actividades).guardarSiCaben(
				org.mockito.ArgumentMatchers.eq(ESTUDIANTE), captura.capture(),
				org.mockito.ArgumentMatchers.eq(ActividadExtra.MAXIMO_POR_ESTUDIANTE));
		assertThat(captura.getValue()).extracting(ActividadExtra::dia)
				.containsExactly(DiaDeLaSemana.LUNES, DiaDeLaSemana.MIERCOLES);
		assertThat(captura.getValue()).allSatisfy(a -> {
			assertThat(a.nombre()).isEqualTo("Baile");
			assertThat(a.idEstudiante()).isEqualTo(ESTUDIANTE);
		});
		assertThat(guardadas).hasSize(2);
	}

	@Test
	void unDiaRepetidoSeGuardaUnaSolaVez() {
		given(actividades.guardarSiCaben(anyInt(), any(), anyInt())).willAnswer(i -> Optional.of(i.getArgument(1)));

		var guardadas = new AnadirActividadExtraServicio(actividades)
				.ejecutar(ESTUDIANTE, datos("Baile", List.of(DiaDeLaSemana.LUNES, DiaDeLaSemana.LUNES)));

		assertThat(guardadas).hasSize(1);
	}

	@Test
	void sinDiasSePideElegirAlMenosUno() {
		assertThatThrownBy(() -> new AnadirActividadExtraServicio(actividades).ejecutar(ESTUDIANTE, datos("Baile", List.of())))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessage("Elige al menos un día");
		verify(actividades, never()).guardarSiCaben(anyInt(), any(), anyInt());
	}

	@Test
	void unaActividadSinSentidoSeRechazaSinTocarLaBase() {
		assertThatThrownBy(() -> new AnadirActividadExtraServicio(actividades)
				.ejecutar(ESTUDIANTE, datos("  ", List.of(DiaDeLaSemana.LUNES))))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		verify(actividades, never()).guardarSiCaben(anyInt(), any(), anyInt());
	}

	@Test
	void siNoCabenSeDiceElLimite() {
		given(actividades.guardarSiCaben(anyInt(), any(), anyInt())).willReturn(Optional.empty());

		assertThatThrownBy(() -> new AnadirActividadExtraServicio(actividades)
				.ejecutar(ESTUDIANTE, datos("Baile", List.of(DiaDeLaSemana.LUNES))))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessage("Puedes tener hasta 20 actividades extra");
	}

	@Test
	void quitarUnaQueNoExisteONoEsDelEstudianteSeDiceComoNoEncontrada() {
		given(actividades.quitar(ESTUDIANTE, 9)).willReturn(false);

		assertThatThrownBy(() -> new QuitarActividadExtraServicio(actividades).ejecutar(ESTUDIANTE, 9))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessage("No encontramos esa actividad");
	}

	@Test
	void quitarUnaExistenteNoLanzaNada() {
		given(actividades.quitar(ESTUDIANTE, 9)).willReturn(true);

		new QuitarActividadExtraServicio(actividades).ejecutar(ESTUDIANTE, 9);

		verify(actividades).quitar(ESTUDIANTE, 9);
	}
}
