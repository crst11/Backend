package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

/** Qué adaptador envía el código según el perfil y si hay servidor de correo configurado. */
class SeleccionDelEnviadorDeCodigoTest {

	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
			.withUserConfiguration(EnviadorDeCodigoEnConsola.class, EnviadorDeCodigoPorCorreo.class)
			.withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
			.withPropertyValues("cundiapp.correo.remitente=cundiapp.pruebas@gmail.com");

	@Test
	void enLocalSinServidorElCodigoSaleEnLaConsola() {
		contexto.withPropertyValues("spring.profiles.active=local", "spring.mail.host=")
				.run(app -> assertThat(app.getBean(EnviadorDeCodigoPort.class)).isInstanceOf(EnviadorDeCodigoEnConsola.class));
	}

	@Test
	void conServidorElCodigoSalePorCorreoAunqueSeaLocal() {
		contexto.withPropertyValues("spring.profiles.active=local", "spring.mail.host=smtp.gmail.com")
				.run(app -> {
					assertThat(app).hasSingleBean(EnviadorDeCodigoPort.class);
					assertThat(app.getBean(EnviadorDeCodigoPort.class)).isInstanceOf(EnviadorDeCodigoPorCorreo.class);
				});
	}

	@Test
	void fueraDeLocalSinServidorNoHayAQuienEnviarYLaAplicacionNoArrancaria() {
		contexto.withPropertyValues("spring.profiles.active=prod", "spring.mail.host=")
				.run(app -> assertThat(app).doesNotHaveBean(EnviadorDeCodigoPort.class));
	}
}
