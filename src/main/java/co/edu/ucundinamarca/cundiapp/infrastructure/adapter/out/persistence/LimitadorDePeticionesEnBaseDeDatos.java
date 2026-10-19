package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDePeticionesPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Peticiones contadas en PostgreSQL con una ventana fija por clave (12 factores, factor 6, SCRUM-216): la primera abre la
 * ventana y, pasado el tiempo, el contador vuelve a cero. Es el limitador en memoria de SCRUM-88 llevado a la base de datos,
 * para que el cupo no se reinicie con cada despliegue ni se multiplique con cada instancia.
 *
 * <p>Cada petición es una sola operación atómica ({@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING}): cuenta y lee el
 * resultado sin que otra instancia se cuele entre las dos cosas. La clave que se guarda es un HMAC, nunca lo que se cuenta.
 */
@Component
class LimitadorDePeticionesEnBaseDeDatos implements LimitadorDePeticionesPort {

	private static final String ESPACIO = "peticiones";

	private record Conteo(int peticiones, Instant primera) {
	}

	private final JdbcTemplate jdbc;
	private final HuellaDeClaves huella;
	private final RelojPort reloj;

	LimitadorDePeticionesEnBaseDeDatos(JdbcTemplate jdbc, HuellaDeClaves huella, RelojPort reloj) {
		this.jdbc = jdbc;
		this.huella = huella;
		this.reloj = reloj;
	}

	@Override
	public Optional<Duration> esperaSiSeExcede(String clave, int maximo, Duration ventana) {
		Instant ahora = reloj.ahora();
		OffsetDateTime ahoraEnUtc = OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC);
		// La ventana sigue abierta mientras su primera petición sea más reciente que «ahora menos la ventana».
		OffsetDateTime corte = OffsetDateTime.ofInstant(ahora.minus(ventana), ZoneOffset.UTC);
		Conteo conteo = jdbc.queryForObject("""
				INSERT INTO limite_de_uso (clave, conteo, primera, ultima) VALUES (?, 1, ?, ?)
				ON CONFLICT (clave) DO UPDATE SET
				    conteo  = CASE WHEN limite_de_uso.primera > ? THEN limite_de_uso.conteo + 1 ELSE 1 END,
				    primera = CASE WHEN limite_de_uso.primera > ? THEN limite_de_uso.primera ELSE EXCLUDED.primera END,
				    ultima  = EXCLUDED.ultima
				RETURNING conteo, primera""",
				(fila, numero) -> new Conteo(fila.getInt("conteo"), fila.getObject("primera", OffsetDateTime.class).toInstant()),
				huella.de(ESPACIO, clave), ahoraEnUtc, ahoraEnUtc, corte, corte);
		return conteo.peticiones() > maximo
				? Optional.of(Duration.between(ahora, conteo.primera().plus(ventana)))
				: Optional.empty();
	}
}
