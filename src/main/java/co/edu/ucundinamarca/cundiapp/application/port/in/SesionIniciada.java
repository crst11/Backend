package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;

/** Lo que recibe quien inicia o renueva sesión: el token de acceso y el de refresco con su vigencia. */
public record SesionIniciada(
		Estudiante estudiante,
		String tokenDeAcceso,
		Instant accesoExpira,
		String tokenDeRefresco,
		Instant refrescoExpira) {
}
