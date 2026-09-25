package co.edu.ucundinamarca.cundiapp.application.port.out;

/** Cuenta los intentos fallidos seguidos por una clave (un correo o una IP) y decide cuándo bloquear. */
public interface LimitadorDeIntentosPort {

	boolean estaBloqueado(String clave);

	void registrarFallo(String clave);

	void reiniciar(String clave);
}
