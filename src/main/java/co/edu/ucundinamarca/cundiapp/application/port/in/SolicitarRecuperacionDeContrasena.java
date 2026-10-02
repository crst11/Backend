package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: pedir un código para definir una contraseña nueva sin saber la anterior (RF01). */
public interface SolicitarRecuperacionDeContrasena {

	/**
	 * Siempre termina igual, exista o no una cuenta con ese correo: quien no tiene cuenta no debe
	 * poder averiguar quién sí la tiene.
	 */
	void ejecutar(String correo);
}
