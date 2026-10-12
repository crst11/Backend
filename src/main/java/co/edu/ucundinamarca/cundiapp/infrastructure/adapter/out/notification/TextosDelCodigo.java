package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;

/**
 * Lo que dice el correo según para qué se pidió el código (SCRUM-68).
 *
 * <p>Separado del envío: quien arma el mensaje no decide cómo se entrega, y cambiar un texto no obliga
 * a tocar el código que habla con el servidor de correo.
 *
 * <p>El aviso del final importa: si alguien recibe un código que no pidió, tiene que entender qué
 * significa y que nadie puede usarlo sin él.
 */
record TextosDelCodigo(String asunto, String titulo, String instruccion, String aviso) {

	static TextosDelCodigo para(PropositoDelCodigo proposito, String codigo) {
		return switch (proposito) {
			case VERIFICAR_CORREO -> new TextosDelCodigo(
					codigo + " es tu código de verificación de CundiApp",
					"Verifica tu correo institucional",
					"Escribe este código en la app para activar tu cuenta:",
					"Si no creaste una cuenta en CundiApp, ignora este correo: nadie podrá usarla sin este código.");
			case RECUPERAR_CONTRASENA -> new TextosDelCodigo(
					codigo + " es tu código para cambiar la contraseña de CundiApp",
					"Cambia tu contraseña",
					"Escribe este código en la app para definir una contraseña nueva:",
					"Si no pediste cambiarla, ignora este correo: tu contraseña actual sigue funcionando "
							+ "y nadie puede cambiarla sin este código.");
		};
	}
}
