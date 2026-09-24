package co.edu.ucundinamarca.cundiapp.infraestructura.salida.seguridad;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.GeneradorDeCodigoPort;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
class GeneradorDeCodigoAleatorio implements GeneradorDeCodigoPort {

	private final SecureRandom azar = new SecureRandom();

	@Override
	public String generar() {
		return String.format("%06d", azar.nextInt(1_000_000));
	}
}
