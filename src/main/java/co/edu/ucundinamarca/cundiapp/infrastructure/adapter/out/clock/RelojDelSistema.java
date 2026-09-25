package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.clock;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class RelojDelSistema implements RelojPort {

	@Override
	public Instant ahora() {
		return Instant.now();
	}
}
