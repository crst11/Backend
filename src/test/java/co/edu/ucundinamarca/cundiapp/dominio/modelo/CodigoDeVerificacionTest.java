package co.edu.ucundinamarca.cundiapp.dominio.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CodigoDeVerificacion.Resultado;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CodigoDeVerificacionTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final CodigoDeVerificacion codigo = CodigoDeVerificacion.emitir(1, "123456", AHORA);

	@Test
	void venceALos15MinutosYNoGuardaElCodigoEnClaro() {
		assertThat(codigo.fechaExpiracion()).isEqualTo(AHORA.plusSeconds(15 * 60));
		assertThat(codigo.huella()).hasSize(64).doesNotContain("123456");
	}

	@Test
	void aceptaElCodigoCorrectoYLoMarcaComoUsado() {
		var intento = codigo.intentar("123456", AHORA.plusSeconds(60));

		assertThat(intento.resultado()).isEqualTo(Resultado.ACEPTADO);
		assertThat(intento.codigo().fechaUso()).isEqualTo(AHORA.plusSeconds(60));
	}

	@Test
	void cuentaLosIntentosFallidos() {
		var intento = codigo.intentar("000000", AHORA.plusSeconds(60));

		assertThat(intento.resultado()).isEqualTo(Resultado.INCORRECTO);
		assertThat(intento.codigo().intentosFallidos()).isEqualTo(1);
		assertThat(intento.codigo().intentosRestantes()).isEqualTo(4);
	}

	@Test
	void trasCincoFallosRechazaInclusoElCodigoCorrecto() {
		var actual = codigo;
		for (int i = 0; i < 5; i++) {
			actual = actual.intentar("000000", AHORA.plusSeconds(60)).codigo();
		}

		var intento = actual.intentar("123456", AHORA.plusSeconds(60));

		assertThat(intento.resultado()).isEqualTo(Resultado.INTENTOS_AGOTADOS);
	}

	@Test
	void rechazaUnCodigoVencido() {
		var intento = codigo.intentar("123456", AHORA.plusSeconds(15 * 60 + 1));

		assertThat(intento.resultado()).isEqualTo(Resultado.VENCIDO);
	}

	@Test
	void unCodigoUsadoNoSirveDeNuevo() {
		var usado = codigo.intentar("123456", AHORA.plusSeconds(60)).codigo();

		assertThat(usado.intentar("123456", AHORA.plusSeconds(120)).resultado()).isEqualTo(Resultado.YA_USADO);
	}

	@Test
	void sePuedePedirOtroSoloSiVencioOAgotoLosIntentos() {
		assertThat(codigo.puedeReemitirse(AHORA.plusSeconds(60))).isFalse();
		assertThat(codigo.puedeReemitirse(AHORA.plusSeconds(15 * 60 + 1))).isTrue();

		var agotado = codigo;
		for (int i = 0; i < 5; i++) {
			agotado = agotado.intentar("000000", AHORA.plusSeconds(60)).codigo();
		}
		assertThat(agotado.puedeReemitirse(AHORA.plusSeconds(60))).isTrue();
	}
}
