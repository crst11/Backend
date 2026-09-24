package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import java.time.Instant;

/** Lo que recibe quien inicia o renueva sesión: el token de acceso y el de refresco con su vigencia. */
public record SesionIniciada(
		Estudiante estudiante,
		String tokenDeAcceso,
		Instant accesoExpira,
		String tokenDeRefresco,
		Instant refrescoExpira) {
}
