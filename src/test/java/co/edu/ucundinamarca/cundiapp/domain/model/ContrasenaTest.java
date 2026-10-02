package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContrasenaTest {

	private static final CorreoInstitucional CORREO = new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co");

	@Test
	void aceptaUnaContrasenaQueCumpleLaPolitica() {
		assertThatCode(() -> new Contrasena("Segura2026!")).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Corta1!",          // menos de 8 caracteres
			"segura2026!",      // sin mayúscula
			"SEGURA2026!",      // sin minúscula
			"SeguraClave!",     // sin número
			"Segura2026",       // sin carácter especial
	})
	void rechazaLaQueIncumpleAlgunRequisito(String candidata) {
		assertThatThrownBy(() -> new Contrasena(candidata))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void elMensajeDiceLaReglaCompletaSinDetallarQueFalta() {
		// Como en Google: una sola frase con la regla. Ir soltando lo que falta de a poco también le
		// diría a quien ataca exactamente cuánto le queda.
		assertThatThrownBy(() -> new Contrasena("corta"))
				.hasMessageContaining("Elige una contraseña más segura")
				.hasMessageContaining(Contrasena.REGLA);
	}

	@Test
	void exigeLaContrasena() {
		assertThatThrownBy(() -> new Contrasena("  "))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("obligatoria");
	}

	@Test
	void rechazaLaQueSuperaElLimiteQueBcryptTieneEnCuenta() {
		String masDe72Bytes = "Aa1!".repeat(19); // 76 bytes

		assertThatThrownBy(() -> new Contrasena(masDe72Bytes))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("72");
	}

	@Test
	void rechazaLaQueContieneElUsuarioDelCorreo() {
		assertThatThrownBy(() -> Contrasena.nueva("Ana.diaz2026!", CORREO))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("usuario del correo");
	}

	@Test
	void unUsuarioMuyCortoNoBloqueaContrasenasRazonables() {
		var correoCorto = new CorreoInstitucional("ana@ucundinamarca.edu.co");

		assertThatCode(() -> Contrasena.nueva("Manana2026!", correoCorto)).doesNotThrowAnyException();
	}

	@Test
	void noRevelaLaContrasenaAlEscribirla() {
		assertThat(new Contrasena("Segura2026!").toString()).doesNotContain("Segura2026!");
	}

	@Test
	void informaQueRequisitosFaltanParaMostrarlosEnLaPantalla() {
		assertThat(Contrasena.requisitosQueFaltan("segura2026!")).containsExactly("una mayúscula");
		assertThat(Contrasena.requisitosQueFaltan("Segura2026!")).isEmpty();
	}
}
