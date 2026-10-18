package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Un aviso (RF10, SCRUM-37): nace sin leer y sin entregar, y no admite datos que la base de datos rechazaría. */
class AvisoTest {

	private static final Instant AHORA = Instant.parse("2026-10-14T15:00:00Z");

	@Test
	void unAvisoNuevoNaceSinLeerNiEntregar() {
		Aviso aviso = Aviso.nuevo(5, TipoDeAviso.VENCIMIENTO_PROXIMO, "Vence pronto", "«Taller» vence hoy.", AHORA);

		assertThat(aviso.id()).isNull();
		assertThat(aviso.leido()).isFalse();
		assertThat(aviso.estadoEnvio()).isEqualTo(EstadoDeEnvio.PENDIENTE);
		assertThat(aviso.intentos()).isZero();
	}

	@Test
	void sinTituloOMensajeSeRechaza() {
		assertThatThrownBy(() -> Aviso.nuevo(5, TipoDeAviso.CAMBIO_RIESGO, " ", "texto", AHORA))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> Aviso.nuevo(5, TipoDeAviso.CAMBIO_RIESGO, "Título", "", AHORA))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> Aviso.nuevo(5, TipoDeAviso.CAMBIO_RIESGO, "a".repeat(121), "texto", AHORA))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void losIntentosNoPasanDelMaximo() {
		assertThatThrownBy(() -> new Aviso(1, 5, TipoDeAviso.CAMBIO_RIESGO, "T", "m", AHORA, EstadoDeEnvio.PENDIENTE, false,
				Aviso.MAXIMO_DE_INTENTOS + 1)).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void losTiposYEstadosSeConviertenDeYHaciaLaBaseDeDatos() {
		assertThat(TipoDeAviso.desdeBd("vencimiento_proximo")).isEqualTo(TipoDeAviso.VENCIMIENTO_PROXIMO);
		assertThat(TipoDeAviso.CAMBIO_RIESGO.valorEnBd()).isEqualTo("cambio_riesgo");
		assertThat(EstadoDeEnvio.desdeBd("fallida")).isEqualTo(EstadoDeEnvio.FALLIDA);
		assertThat(EstadoDeEnvio.ENVIADA.valorEnBd()).isEqualTo("enviada");
		assertThatThrownBy(() -> TipoDeAviso.desdeBd("otro")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> EstadoDeEnvio.desdeBd("otro")).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}
}
