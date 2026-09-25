package co.edu.ucundinamarca.cundiapp.dominio.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SesionTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final Sesion sesion = Sesion.abrir(1, "token-de-refresco", AHORA, Duration.ofDays(7), "Firefox", "10.0.0.1");

	@Test
	void guardaLaHuellaDelTokenYNoElTokenEnClaro() {
		assertThat(sesion.huellaRefresco()).hasSize(64).doesNotContain("token-de-refresco");
		assertThat(sesion.huellaRefresco()).isEqualTo(Sesion.huellaDe("token-de-refresco"));
	}

	@Test
	void vence7DiasDespuesDeAbrirse() {
		assertThat(sesion.fechaExpiracion()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
		assertThat(sesion.estaVigente(AHORA.plus(Duration.ofDays(6)))).isTrue();
		assertThat(sesion.estaVigente(AHORA.plus(Duration.ofDays(7)))).isFalse();
	}

	@Test
	void unaSesionRevocadaYaNoEstaVigente() {
		var revocada = sesion.revocar(MotivoDeRevocacion.CIERRE_SESION, AHORA.plusSeconds(60));

		assertThat(revocada.estaRevocada()).isTrue();
		assertThat(revocada.estaVigente(AHORA.plusSeconds(120))).isFalse();
		assertThat(revocada.fueRotada()).isFalse();
	}

	@Test
	void distingueLaRevocacionPorRotacion() {
		assertThat(sesion.revocar(MotivoDeRevocacion.ROTACION, AHORA).fueRotada()).isTrue();
	}

	@Test
	void elMotivoSeConvierteDeIdaYVueltaConElValorDeLaBaseDeDatos() {
		for (MotivoDeRevocacion motivo : MotivoDeRevocacion.values()) {
			assertThat(MotivoDeRevocacion.desdeBd(motivo.valorEnBd())).isEqualTo(motivo);
		}
		assertThatThrownBy(() -> MotivoDeRevocacion.desdeBd("otro")).isInstanceOf(IllegalArgumentException.class);
	}
}
