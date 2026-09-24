package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Sesion;
import java.time.Instant;
import java.util.Optional;

public interface SesionRepositorio {

	/** Guarda una sesión nueva; el repositorio le asigna el consecutivo dentro del estudiante. */
	void guardar(Sesion nueva);

	Optional<Sesion> buscarPorHuella(String huellaRefresco);

	/** En una sola operación: deja revocada la sesión usada y abre la que la reemplaza. */
	void rotar(Sesion revocada, Sesion nueva);

	void actualizar(Sesion sesion);

	/** Revoca todas las sesiones que sigan abiertas del estudiante. */
	void revocarVigentes(int idEstudiante, MotivoDeRevocacion motivo, Instant ahora);
}
