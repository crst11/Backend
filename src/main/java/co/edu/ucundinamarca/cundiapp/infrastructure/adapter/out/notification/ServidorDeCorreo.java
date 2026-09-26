package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Decide qué adaptador envía el código: con CORREO_SMTP_HOST definido va por correo real; sin él, en
 * local se imprime en la consola. Una línea CORREO_SMTP_HOST= vacía en el .env cuenta como no definido.
 */
final class ServidorDeCorreo {

	private static final String PROPIEDAD = "spring.mail.host";

	private ServidorDeCorreo() {
	}

	static boolean estaConfigurado(ConditionContext contexto) {
		return StringUtils.hasText(contexto.getEnvironment().getProperty(PROPIEDAD));
	}

	static final class Configurado implements Condition {

		@Override
		public boolean matches(ConditionContext contexto, AnnotatedTypeMetadata metadatos) {
			return estaConfigurado(contexto);
		}
	}

	static final class SinConfigurar implements Condition {

		@Override
		public boolean matches(ConditionContext contexto, AnnotatedTypeMetadata metadatos) {
			return !estaConfigurado(contexto);
		}
	}
}
