package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SesionRepositorio {

	/**
	 * Guarda una sesión nueva; el repositorio le asigna el consecutivo dentro del estudiante y
	 * devuelve la sesión ya con él, porque el token de acceso lo lleva dentro (SCRUM-73).
	 */
	Sesion guardar(Sesion nueva);

	Optional<Sesion> buscarPorHuella(String huellaRefresco);

	/**
	 * ¿Esa sesión sigue abierta y sin vencer? Se pregunta en cada petición protegida para que cerrar
	 * una sesión corte el acceso de inmediato y no cuando venza su token (SCRUM-77). Es una búsqueda
	 * por clave primaria.
	 */
	boolean sigueVigente(int idEstudiante, int consecutivo, Instant ahora);

	/** En una sola operación: deja revocada la sesión usada y abre la que la reemplaza. */
	Sesion rotar(Sesion revocada, Sesion nueva);

	void actualizar(Sesion sesion);

	/** Las sesiones del estudiante que siguen abiertas y sin vencer, de la más reciente a la más antigua. */
	List<Sesion> listarVigentes(int idEstudiante, Instant ahora);

	/** Revoca una sesión concreta si sigue abierta. Devuelve si la encontró abierta y la revocó. */
	boolean revocarUna(int idEstudiante, int consecutivo, MotivoDeRevocacion motivo, Instant ahora);

	/** Revoca todas las sesiones que sigan abiertas del estudiante. */
	void revocarVigentes(int idEstudiante, MotivoDeRevocacion motivo, Instant ahora);
}
