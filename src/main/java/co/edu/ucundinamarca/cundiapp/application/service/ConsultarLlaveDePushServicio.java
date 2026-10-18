package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarLlaveDePush;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort;
import java.util.Optional;

/** La llave pública VAPID para que el navegador se suscriba (RF10, SCRUM-37). */
public class ConsultarLlaveDePushServicio implements ConsultarLlaveDePush {

	private final EnviadorDePushPort enviador;

	public ConsultarLlaveDePushServicio(EnviadorDePushPort enviador) {
		this.enviador = enviador;
	}

	@Override
	public Optional<String> ejecutar() {
		return enviador.llavePublica();
	}
}
