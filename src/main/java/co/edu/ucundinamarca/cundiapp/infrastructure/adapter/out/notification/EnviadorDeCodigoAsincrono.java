package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Saca el envío del código fuera de la petición HTTP (SCRUM-67).
 *
 * <p>Conectarse a Gmail, negociar TLS, autenticar y entregar el mensaje tardaba segundos que la
 * persona esperaba mirando el formulario. Ahora el registro responde apenas la cuenta queda guardada
 * y el correo sale en segundo plano.
 *
 * <p>Es un decorador del puerto: envuelve al adaptador que toque (SMTP o consola) sin modificarlo y
 * sin que la capa de aplicación se entere de que hay hilos de por medio.
 *
 * <p>Un envío en segundo plano ya no puede avisar su falla a tiempo, así que la registra en el log.
 * Quien no reciba el código puede pedir otro desde la pantalla de verificación.
 */
@Component(EnvioDelCodigo.ASINCRONO)
class EnviadorDeCodigoAsincrono implements EnviadorDeCodigoPort {

	private static final Logger log = LoggerFactory.getLogger(EnviadorDeCodigoAsincrono.class);

	private final EnviadorDeCodigoPort directo;

	EnviadorDeCodigoAsincrono(@Qualifier(EnvioDelCodigo.DIRECTO) EnviadorDeCodigoPort directo) {
		this.directo = directo;
	}

	@Override
	@Async(EnvioDelCodigo.EJECUTOR)
	public void enviar(CorreoInstitucional destino, String codigo, PropositoDelCodigo proposito) {
		try {
			directo.enviar(destino, codigo, proposito);
		} catch (RuntimeException e) {
			// Solo el tipo de falla: el mensaje puede traer la dirección del destinatario.
			log.error("No se pudo enviar el código en segundo plano ({})", e.getClass().getSimpleName());
		}
	}
}
