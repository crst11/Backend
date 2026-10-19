package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.EntregarAvisosPendientes;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.DispositivoConAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort.CargaDePush;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeEnvioPush;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Entrega por Web Push los avisos que todavía no salieron (RF10, SCRUM-37).
 *
 * <p>Cada aviso se manda a todos los dispositivos activos de su dueño. Queda «enviado» si llegó a al menos
 * uno; «pendiente» si todos fallaron de forma pasajera (se reintenta en la siguiente ronda, pocas veces);
 * y «fallido» si no hay a quién entregárselo o ya no quedan intentos. En ningún caso desaparece: el aviso
 * siempre se ve dentro de la aplicación.
 */
public class EntregarAvisosPendientesServicio implements EntregarAvisosPendientes {

	/** Cuántos avisos se entregan por ronda: la tarea corre cada pocos minutos, no hay prisa por vaciar la cola de golpe. */
	private static final int AVISOS_POR_RONDA = 50;

	/**
	 * Cuánto puede durar una ronda como máximo. Cada envío espera hasta 10 s a un servicio de push, y 50 avisos con varios dispositivos
	 * cada uno podían sumar minutos. La ronda corre con un candado que ocupa una conexión (SCRUM-228): se acota para que no se alargue.
	 */
	private static final Duration PRESUPUESTO_DE_LA_RONDA = Duration.ofSeconds(90);

	/** Un «vence pronto» que el dispositivo recibe horas después, sin conexión, ya no sirve: el servicio lo descarta. */
	private static final Duration VIGENCIA_DEL_AVISO = Duration.ofHours(6);

	private final AvisoRepositorio avisos;
	private final DispositivoConAvisosRepositorio dispositivos;
	private final EnviadorDePushPort enviador;
	private final RelojPort reloj;

	public EntregarAvisosPendientesServicio(
			AvisoRepositorio avisos,
			DispositivoConAvisosRepositorio dispositivos,
			EnviadorDePushPort enviador,
			RelojPort reloj) {
		this.avisos = avisos;
		this.dispositivos = dispositivos;
		this.enviador = enviador;
		this.reloj = reloj;
	}

	@Override
	public int ejecutar() {
		int entregados = 0;
		Instant limite = reloj.ahora().plus(PRESUPUESTO_DE_LA_RONDA);
		for (Aviso aviso : avisos.porEntregar(AVISOS_POR_RONDA)) {
			// Un servicio de push lento no puede alargar la ronda sin fin (SCRUM-228): lo que no alcanzó queda «pendiente» y sale en la
			// siguiente ronda, que es de unos minutos.
			if (!reloj.ahora().isBefore(limite)) {
				break;
			}
			if (entregar(aviso)) {
				entregados++;
			}
		}
		return entregados;
	}

	private boolean entregar(Aviso aviso) {
		int id = aviso.id();
		if (!enviador.disponible()) {
			avisos.registrarFallo(id, true);
			return false;
		}
		List<DispositivoConAvisos> destinos = dispositivos.activos(aviso.idEstudiante());
		if (destinos.isEmpty()) {
			avisos.registrarFallo(id, true);
			return false;
		}
		CargaDePush carga = new CargaDePush(aviso.titulo(), aviso.mensaje(), rutaDe(aviso));
		boolean llego = false;
		boolean sePuedeReintentar = false;
		for (DispositivoConAvisos destino : destinos) {
			ResultadoDeEnvioPush resultado = enviador.enviar(destino.suscripcion(), carga, VIGENCIA_DEL_AVISO);
			switch (resultado) {
				case ENTREGADO -> llego = true;
				case SUSCRIPCION_MUERTA -> dispositivos.desactivar(aviso.idEstudiante(), destino.consecutivo());
				case FALLO_PASAJERO -> sePuedeReintentar = true;
			}
		}
		if (llego) {
			avisos.marcarEnviado(id, reloj.ahora());
			return true;
		}
		avisos.registrarFallo(id, !sePuedeReintentar);
		return false;
	}

	/** A dónde lleva la notificación al tocarla. */
	private static String rutaDe(Aviso aviso) {
		return switch (aviso.tipo()) {
			case VENCIMIENTO_PROXIMO, PENDIENTE_VENCIDO -> "/pendientes";
			case CAMBIO_RIESGO -> "/brujula";
		};
	}
}
