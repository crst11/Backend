package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;

/** Caso de uso: confirmar el correo institucional con el código recibido y activar la cuenta (RF01). */
public interface VerificarCorreo {

	Estudiante ejecutar(String correo, String codigo);
}
