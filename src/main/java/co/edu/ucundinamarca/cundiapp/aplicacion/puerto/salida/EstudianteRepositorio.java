package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import java.util.Optional;

public interface EstudianteRepositorio {

	boolean existeCuentaCon(CorreoInstitucional correo);

	Optional<Estudiante> buscarPorCorreo(CorreoInstitucional correo);

	/** Guarda la cuenta y su credencial local en una sola operación y devuelve la cuenta con id. */
	Estudiante guardarConCredencialLocal(Estudiante estudiante, String hashContrasena);

	/** Deja la cuenta activa y marca como verificado el correo de su credencial local. */
	void guardarActivacion(Estudiante activado);
}
