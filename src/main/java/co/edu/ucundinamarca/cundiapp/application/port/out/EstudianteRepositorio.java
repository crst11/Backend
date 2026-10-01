package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;
import java.util.Optional;

public interface EstudianteRepositorio {

	Optional<Estudiante> buscarPorCorreo(CorreoInstitucional correo);

	Optional<Estudiante> buscarPorId(int idEstudiante);

	/** La contraseña cifrada de la credencial local, si el estudiante tiene una. */
	Optional<String> contrasenaCifradaDe(int idEstudiante);

	/** Guarda la cuenta y su credencial local en una sola operación y devuelve la cuenta con id. */
	Estudiante guardarConCredencialLocal(Estudiante estudiante, String hashContrasena);

	/**
	 * Registrarse de nuevo con el correo de una cuenta que se había eliminado (RF01): reescribe la
	 * misma fila en vez de duplicarla, porque el correo ya no está libre para un alta nueva.
	 */
	Estudiante reactivarConCredencialLocal(Estudiante estudiante, String hashContrasena);

	/** Deja la cuenta activa y marca como verificado el correo de su credencial local. */
	void guardarActivacion(Estudiante activado);

	/** Cambia la contraseña de la credencial local (RF01: recuperar la contraseña). */
	void cambiarContrasenaLocal(int idEstudiante, String hashContrasena);

	/** Deja la cuenta inactiva (RF01: eliminar cuenta), sin tocar sus credenciales. */
	void guardarDesactivacion(Estudiante desactivado);

	void registrarUltimoAcceso(int idEstudiante, Instant fecha);
}
