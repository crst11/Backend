package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada;

/** Caso de uso: pedir un código nuevo cuando el anterior venció o agotó sus intentos (RF01). */
public interface ReenviarCodigoDeVerificacion {

	void ejecutar(String correo);
}
