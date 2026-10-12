package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: cerrar una sesión concreta o todas a la vez (RF01). */
public interface CerrarSesionEnDispositivo {

	/** Cierra la sesión indicada. Si ya estaba cerrada o no existe, se rechaza. */
	void ejecutar(int idEstudiante, int consecutivo);

	/**
	 * Cierra todas las sesiones abiertas, incluida la que hace la petición: quien usa esta opción suele
	 * sospechar que alguien más entró, y dejar viva la suya le obligaría a cerrarla aparte.
	 */
	void ejecutarTodas(int idEstudiante);
}
