package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import co.edu.ucundinamarca.cundiapp.application.port.out.DispositivoConAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciasDeAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Configurar mis avisos y ver y quitar mis dispositivos (RF10, SCRUM-38). */
@ExtendWith(MockitoExtension.class)
class PreferenciasYDispositivosServiciosTest {

	private static final int ESTUDIANTE = 5;
	private static final ClavesDePushDePrueba.Suscripcion CLAVES = ClavesDePushDePrueba.nueva();

	@Mock
	private PreferenciasDeAvisosRepositorio preferencias;

	@Mock
	private DispositivoConAvisosRepositorio dispositivos;

	@Test
	void consultaLasPreferenciasGuardadas() {
		PreferenciasDeAvisos guardadas = new PreferenciasDeAvisos(true, false, true, 48);
		given(preferencias.de(ESTUDIANTE)).willReturn(guardadas);

		assertThat(new ConsultarMisPreferenciasDeAvisosServicio(preferencias).ejecutar(ESTUDIANTE)).isEqualTo(guardadas);
	}

	@Test
	void cambiarLasGuardaYDevuelveLasNuevas() {
		PreferenciasDeAvisos nuevas = new PreferenciasDeAvisos(true, true, false, 72);

		PreferenciasDeAvisos resultado = new CambiarMisPreferenciasDeAvisosServicio(preferencias).ejecutar(ESTUDIANTE, nuevas);

		verify(preferencias).guardar(ESTUDIANTE, nuevas);
		assertThat(resultado).isEqualTo(nuevas);
	}

	@Test
	void cambiarSinPreferenciasSeRechazaSinEscribir() {
		assertThatThrownBy(() -> new CambiarMisPreferenciasDeAvisosServicio(preferencias).ejecutar(ESTUDIANTE, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);

		verify(preferencias, never()).guardar(anyInt(), any());
	}

	@Test
	void listaLosDispositivosQueHoyRecibenAvisos() {
		SuscripcionPush suscripcion = new SuscripcionPush("https://fcm.googleapis.com/fcm/send/a", CLAVES.clavePublica(), CLAVES.secretoEnBase64());
		List<DispositivoConAvisos> activos = List.of(new DispositivoConAvisos(1, "Chrome en Android", suscripcion, true, Instant.now()));
		given(dispositivos.activos(ESTUDIANTE)).willReturn(activos);

		assertThat(new ListarMisDispositivosConAvisosServicio(dispositivos).ejecutar(ESTUDIANTE)).isEqualTo(activos);
	}

	@Test
	void quitarUnDispositivoMioFunciona() {
		given(dispositivos.quitar(ESTUDIANTE, 2)).willReturn(true);

		new QuitarMiDispositivoConAvisosServicio(dispositivos).ejecutar(ESTUDIANTE, 2);

		verify(dispositivos).quitar(ESTUDIANTE, 2);
	}

	@Test
	void quitarUnDispositivoAjenoOInexistenteRespondeIgual() {
		given(dispositivos.quitar(ESTUDIANTE, 99)).willReturn(false);

		assertThatThrownBy(() -> new QuitarMiDispositivoConAvisosServicio(dispositivos).ejecutar(ESTUDIANTE, 99))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("No encontramos ese dispositivo");
	}
}
