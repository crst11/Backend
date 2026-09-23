package co.edu.ucundinamarca.cundiapp.infraestructura.salida.tiempo;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class RelojDelSistema implements RelojPort {

	@Override
	public Instant ahora() {
		return Instant.now();
	}
}
