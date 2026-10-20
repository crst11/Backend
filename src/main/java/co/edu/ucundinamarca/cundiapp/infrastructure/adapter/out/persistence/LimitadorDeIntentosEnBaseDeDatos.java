package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDeIntentosPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Intentos fallidos seguidos, contados en PostgreSQL (12 factores, factor 6, SCRUM-216). Hace lo mismo que el
 * limitador en memoria al que reemplaza: un correo se bloquea a los 5 fallos y una IP a los 20 (en la universidad
 * muchos estudiantes comparten la red), el bloqueo dura 15 minutos desde el último fallo y después el contador vuelve a
 * cero. La diferencia es que **sobrevive a un despliegue y lo comparten todas las instancias**.
 *
 * <p>Registrar un fallo es una sola operación atómica ({@code INSERT ... ON CONFLICT DO UPDATE}): dos instancias que
 * fallan a la vez suman las dos. La clave que se guarda es un HMAC (ver {@link HuellaDeClaves}), nunca el correo ni la IP.
 */
@Component
class LimitadorDeIntentosEnBaseDeDatos implements LimitadorDeIntentosPort {

	static final int MAXIMO_POR_CORREO = 5;
	static final int MAXIMO_POR_IP = 20;
	static final Duration VENTANA = Duration.ofMinutes(15);
	private static final String ESPACIO = "intentos";

	private final JdbcTemplate jdbc;
	private final HuellaDeClaves huella;
	private final RelojPort reloj;

	LimitadorDeIntentosEnBaseDeDatos(JdbcTemplate jdbc, HuellaDeClaves huella, RelojPort reloj) {
		this.jdbc = jdbc;
		this.huella = huella;
		this.reloj = reloj;
	}

	@Override
	public boolean estaBloqueado(String clave) {
		Integer fallos = jdbc.query(
				"SELECT conteo FROM limite_de_uso WHERE clave = ? AND ultima >= ?",
				resultado -> resultado.next() ? resultado.getInt(1) : null,
				huella.de(ESPACIO, clave), enUtc(reloj.ahora().minus(VENTANA)));
		return fallos != null && fallos >= maximoPara(clave);
	}

	@Override
	public void registrarFallo(String clave) {
		Instant ahora = reloj.ahora();
		OffsetDateTime ahoraEnUtc = enUtc(ahora);
		OffsetDateTime desde = enUtc(ahora.minus(VENTANA));
		// Si el último fallo fue hace menos de 15 minutos la cuenta sigue y suma; si no, empieza de nuevo.
		// «ultima» nunca retrocede (GREATEST): dos fallos casi simultáneos pueden llegar con horas unas millonésimas desordenadas y
		// ck_limite_de_uso_orden rechazaba al segundo (SCRUM-236).
		jdbc.update("""
				INSERT INTO limite_de_uso (clave, conteo, primera, ultima) VALUES (?, 1, ?, ?)
				ON CONFLICT (clave) DO UPDATE SET
				    conteo  = CASE WHEN limite_de_uso.ultima >= ? THEN limite_de_uso.conteo + 1 ELSE 1 END,
				    primera = CASE WHEN limite_de_uso.ultima >= ? THEN limite_de_uso.primera ELSE EXCLUDED.primera END,
				    ultima  = GREATEST(limite_de_uso.ultima, EXCLUDED.ultima)""",
				huella.de(ESPACIO, clave), ahoraEnUtc, ahoraEnUtc, desde, desde);
	}

	@Override
	public void reiniciar(String clave) {
		jdbc.update("DELETE FROM limite_de_uso WHERE clave = ?", huella.de(ESPACIO, clave));
	}

	private static int maximoPara(String clave) {
		return clave.startsWith("ip:") ? MAXIMO_POR_IP : MAXIMO_POR_CORREO;
	}

	private static OffsetDateTime enUtc(Instant instante) {
		return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
	}
}
