package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Solo para desarrollo local sin servidor de correo configurado: imprime el código en el log en vez
 * de enviarlo. PRE y PROD no lo cargan nunca: allí es obligatorio el SMTP (EnviadorDeCodigoPorCorreo).
 */
@Component
@Profile("local")
@Conditional(ServidorDeCorreo.SinConfigurar.class)
class EnviadorDeCodigoEnConsola implements EnviadorDeCodigoPort {

	private static final Logger log = LoggerFactory.getLogger(EnviadorDeCodigoEnConsola.class);

	@Override
	public void enviar(CorreoInstitucional destino, String codigo) {
		log.info("[DEV] Código de verificación para {}: {}", destino.valor(), codigo);
	}
}
