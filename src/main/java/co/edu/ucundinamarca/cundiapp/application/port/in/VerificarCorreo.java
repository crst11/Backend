package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;

/** Caso de uso: confirmar el correo institucional con el código recibido y activar la cuenta (RF01). */
public interface VerificarCorreo {

	Estudiante ejecutar(String correo, String codigo);
}
