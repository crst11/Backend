package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDeIntentosPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Intentos fallidos seguidos, contados en memoria (basta para un solo backend). Un correo se bloquea
 * a los 5 fallos; una IP a los 20, porque en la universidad muchos estudiantes comparten la misma red.
 * El bloqueo dura 15 minutos desde el último fallo; después el contador vuelve a cero.
 */
@Component
class LimitadorDeIntentosEnMemoria implements LimitadorDeIntentosPort {

	static final int MAXIMO_POR_CORREO = 5;
	static final int MAXIMO_POR_IP = 20;
	static final Duration VENTANA = Duration.ofMinutes(15);
	private static final int TAMANO_PARA_LIMPIAR = 10_000;

	private record Registro(int fallos, Instant ultimoFallo) {
	}

	private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
	private final RelojPort reloj;

	LimitadorDeIntentosEnMemoria(RelojPort reloj) {
		this.reloj = reloj;
	}

	@Override
	public boolean estaBloqueado(String clave) {
		Registro registro = registros.get(clave);
		if (registro == null) {
			return false;
		}
		if (haVencido(registro)) {
			registros.remove(clave, registro);
			return false;
		}
		return registro.fallos() >= maximoPara(clave);
	}

	@Override
	public void registrarFallo(String clave) {
		Instant ahora = reloj.ahora();
		registros.compute(clave, (k, actual) ->
				actual == null || haVencido(actual) ? new Registro(1, ahora) : new Registro(actual.fallos() + 1, ahora));
		if (registros.size() > TAMANO_PARA_LIMPIAR) {
			registros.values().removeIf(this::haVencido);
		}
	}

	@Override
	public void reiniciar(String clave) {
		registros.remove(clave);
	}

	private boolean haVencido(Registro registro) {
		return reloj.ahora().isAfter(registro.ultimoFallo().plus(VENTANA));
	}

	private static int maximoPara(String clave) {
		return clave.startsWith("ip:") ? MAXIMO_POR_IP : MAXIMO_POR_CORREO;
	}
}
