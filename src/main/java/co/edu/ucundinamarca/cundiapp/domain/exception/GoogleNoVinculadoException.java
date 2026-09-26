package co.edu.ucundinamarca.cundiapp.domain.exception;

/** Se entró con una cuenta de Google que ningún estudiante ha vinculado todavía. */
public class GoogleNoVinculadoException extends RuntimeException {

	public GoogleNoVinculadoException() {
		super("Tu cuenta de Google no está vinculada a CundiApp. Inicia sesión con tu correo institucional y vincúlala desde Mi cuenta");
	}
}
