package co.edu.ucundinamarca.cundiapp;

import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeEnvioPush;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * El envío de Web Push para las pruebas de integración (RF10): no llama a nadie, anota cada envío y responde
 * lo que la prueba le haya pedido. Las pruebas lo ajustan y lo limpian con {@link #reiniciar()}.
 */
public class EnviadorDePushDePrueba implements EnviadorDePushPort {

	/** Un envío que se le pidió. */
	public record Envio(SuscripcionPush suscripcion, CargaDePush carga, Duration vigencia) {
	}

	private final List<Envio> envios = new CopyOnWriteArrayList<>();
	private volatile boolean disponible = true;
	private volatile ResultadoDeEnvioPush resultado = ResultadoDeEnvioPush.ENTREGADO;

	@Override
	public boolean disponible() {
		return disponible;
	}

	@Override
	public Optional<String> llavePublica() {
		return disponible ? Optional.of("llave-publica-de-prueba") : Optional.empty();
	}

	@Override
	public ResultadoDeEnvioPush enviar(SuscripcionPush suscripcion, CargaDePush carga, Duration vigencia) {
		envios.add(new Envio(suscripcion, carga, vigencia));
		return resultado;
	}

	public List<Envio> envios() {
		return List.copyOf(envios);
	}

	public void responder(ResultadoDeEnvioPush nuevo) {
		this.resultado = nuevo;
	}

	public void estaDisponible(boolean valor) {
		this.disponible = valor;
	}

	/** Vuelve al estado normal: disponible, entrega todo y no recuerda nada. */
	public void reiniciar() {
		envios.clear();
		disponible = true;
		resultado = ResultadoDeEnvioPush.ENTREGADO;
	}
}
