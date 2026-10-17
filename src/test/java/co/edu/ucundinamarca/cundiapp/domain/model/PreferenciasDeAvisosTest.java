package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import org.junit.jupiter.api.Test;

/** Las preferencias de avisos (RF10, SCRUM-38): lo que la persona puede elegir y lo que no. */
class PreferenciasDeAvisosTest {

	@Test
	void porDefectoTodoEstaEncendidoYSeAvisa24HorasAntes() {
		PreferenciasDeAvisos predeterminadas = PreferenciasDeAvisos.PREDETERMINADAS;

		assertThat(predeterminadas.activos()).isTrue();
		assertThat(predeterminadas.vencimientos()).isTrue();
		assertThat(predeterminadas.cambioDeRiesgo()).isTrue();
		assertThat(predeterminadas.anticipacionEnHoras()).isEqualTo(24);
	}

	@Test
	void aceptaLosExtremosDeLaAnticipacion() {
		assertThat(new PreferenciasDeAvisos(true, true, true, 1).anticipacionEnHoras()).isEqualTo(1);
		assertThat(new PreferenciasDeAvisos(true, true, true, 168).anticipacionEnHoras()).isEqualTo(168);
	}

	@Test
	void unaAnticipacionFueraDeUnaHoraAUnaSemanaSeRechazaConUnMensajeUtil() {
		assertThatThrownBy(() -> new PreferenciasDeAvisos(true, true, true, 0))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("entre 1 y 168 horas");
		assertThatThrownBy(() -> new PreferenciasDeAvisos(true, true, true, 169))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new PreferenciasDeAvisos(true, true, true, -5))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void sePuedenApagarLosTiposSinTocarLaAnticipacion() {
		PreferenciasDeAvisos soloRiesgo = new PreferenciasDeAvisos(true, false, true, 48);

		assertThat(soloRiesgo.vencimientos()).isFalse();
		assertThat(soloRiesgo.cambioDeRiesgo()).isTrue();
		assertThat(soloRiesgo.anticipacionEnHoras()).isEqualTo(48);
	}
}
