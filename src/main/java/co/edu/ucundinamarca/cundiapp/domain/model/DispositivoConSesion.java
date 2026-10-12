package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Las sesiones vigentes de un mismo dispositivo, vistas como una sola (RF01, SCRUM-73).
 *
 * <p>Cada renovación del token de acceso rota el refresco y abre otra sesión, así que un navegador
 * que se queda abierto deja una fila nueva cada 20 minutos. Esa rotación se queda como está: es lo
 * que permite descubrir que alguien copió un token. Lo que cambia es la lectura: al estudiante no
 * le sirve ver veinte líneas de su propio portátil, le sirve ver su portátil.
 *
 * <p>El dispositivo es el user agent. La IP no agrupa: cambiar de wifi a datos móviles no convierte
 * un teléfono en otro.
 */
public record DispositivoConSesion(
		String dispositivo,
		String ip,
		MetodoDeAcceso metodo,
		Instant primerAcceso,
		Instant ultimoAcceso,
		Instant expira,
		List<Integer> sesiones,
		boolean esLaActual) {

	public DispositivoConSesion {
		sesiones = List.copyOf(sesiones);
	}

	/**
	 * Agrupa las sesiones vigentes del estudiante.
	 *
	 * @param sesionActual el consecutivo de la sesión desde la que se está mirando, o nulo si no se
	 *     sabe: los tokens emitidos antes de SCRUM-73 no lo traen, y entonces no se marca ninguna.
	 */
	public static List<DispositivoConSesion> agrupar(List<Sesion> vigentes, Integer sesionActual) {
		Map<String, List<Sesion>> porDispositivo = new LinkedHashMap<>();
		for (Sesion sesion : vigentes) {
			porDispositivo.computeIfAbsent(clave(sesion), clave -> new ArrayList<>()).add(sesion);
		}

		List<DispositivoConSesion> dispositivos = porDispositivo.values().stream()
				.map(cadena -> deLaCadena(cadena, sesionActual))
				.sorted(Comparator.comparing(DispositivoConSesion::esLaActual).reversed()
						.thenComparing(DispositivoConSesion::ultimoAcceso, Comparator.reverseOrder()))
				.toList();

		return dispositivos;
	}

	/** Sin user agent no hay con qué distinguirlas, así que todas esas caen en el mismo grupo. */
	private static String clave(Sesion sesion) {
		return sesion.userAgent() == null ? "" : sesion.userAgent();
	}

	private static DispositivoConSesion deLaCadena(List<Sesion> cadena, Integer sesionActual) {
		List<Sesion> enOrden = cadena.stream().sorted(Comparator.comparing(Sesion::fechaInicio)).toList();
		Sesion primera = enOrden.getFirst();
		Sesion ultima = enOrden.getLast();

		return new DispositivoConSesion(
				ultima.userAgent(),
				ultima.ipOrigen(),
				ultima.metodo(),
				primera.fechaInicio(),
				ultima.fechaInicio(),
				ultima.fechaExpiracion(),
				enOrden.stream().map(Sesion::consecutivo).toList(),
				enOrden.stream().anyMatch(sesion -> Objects.equals(sesion.consecutivo(), sesionActual)));
	}

	/** Con cuál consecutivo lo pide cerrar la pantalla: el de su sesión más reciente. */
	public int consecutivo() {
		return sesiones.getLast();
	}
}
