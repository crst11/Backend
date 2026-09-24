package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida;

/** Abstrae el algoritmo de cifrado de contraseñas (bcrypt en infraestructura). */
public interface CifradorDeContrasenaPort {

	String cifrar(String contrasenaSinCifrar);

	boolean coincide(String contrasenaSinCifrar, String contrasenaCifrada);
}
