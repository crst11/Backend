package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoNoEnviadoException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/** El adaptador arma el correo y traduce las fallas del servidor, sin conectarse a ningún SMTP real. */
class EnviadorDeCodigoPorCorreoTest {

	private static final CorreoInstitucional DESTINO = new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co");

	private final JavaMailSender servidor = mock(JavaMailSender.class);
	private final EnviadorDeCodigoPorCorreo enviador =
			new EnviadorDeCodigoPorCorreo(servidor, "cundiapp.pruebas@gmail.com");

	@BeforeEach
	void configurar() {
		given(servidor.createMimeMessage()).willAnswer(invocacion -> new MimeMessage((Session) null));
	}

	@Test
	void enviaElCodigoAlCorreoInstitucionalConVersionHtmlYDeTexto() throws Exception {
		enviador.enviar(DESTINO, "482913");

		var enviado = ArgumentCaptor.forClass(MimeMessage.class);
		verify(servidor).send(enviado.capture());
		MimeMessage mensaje = enviado.getValue();
		assertThat(mensaje.getAllRecipients()).extracting(Object::toString).containsExactly("ana.diaz@ucundinamarca.edu.co");
		assertThat(mensaje.getFrom()[0].toString()).isEqualTo("CundiApp <cundiapp.pruebas@gmail.com>");
		assertThat(mensaje.getSubject()).isEqualTo("482913 es tu código de verificación de CundiApp");

		var crudo = new ByteArrayOutputStream();
		mensaje.writeTo(crudo);
		assertThat(crudo.toString(StandardCharsets.UTF_8)).contains("text/plain", "text/html", "482913");
	}

	@Test
	void laPlantillaTraeElCodigoYCuantoDuraEnLasDosVersiones() {
		assertThat(EnviadorDeCodigoPorCorreo.html("482913")).contains("482913", "vence en 15 minutos");
		assertThat(EnviadorDeCodigoPorCorreo.textoPlano("482913")).contains("482913", "Vence en 15 minutos");
	}

	@Test
	void unaFallaDelServidorSeConvierteEnCorreoNoEnviadoSinExponerLaDireccion() {
		doThrow(new MailSendException("Invalid Addresses: ana.diaz@ucundinamarca.edu.co"))
				.when(servidor).send(any(MimeMessage.class));

		assertThatThrownBy(() -> enviador.enviar(DESTINO, "482913"))
				.isInstanceOf(CorreoNoEnviadoException.class)
				.hasCauseInstanceOf(MailSendException.class)
				.hasMessage("No pudimos enviar el código a tu correo. Intenta de nuevo en unos minutos");
	}

	@Test
	void unUsuarioOClaveRechazadosTambienSonCorreoNoEnviado() {
		doThrow(new MailAuthenticationException("535 Username and Password not accepted"))
				.when(servidor).send(any(MimeMessage.class));

		assertThatThrownBy(() -> enviador.enviar(DESTINO, "482913"))
				.isInstanceOf(CorreoNoEnviadoException.class)
				.hasCauseInstanceOf(MailAuthenticationException.class);
	}
}
