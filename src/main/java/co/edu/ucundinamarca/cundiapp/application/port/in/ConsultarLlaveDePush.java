package co.edu.ucundinamarca.cundiapp.application.port.in;

import java.util.Optional;

/** La llave pública con la que el navegador se suscribe a los avisos (RF10, SCRUM-37). Vacía si el servidor no tiene Web Push. */
public interface ConsultarLlaveDePush {

	Optional<String> ejecutar();
}
