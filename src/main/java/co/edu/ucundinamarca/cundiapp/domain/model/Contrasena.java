package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Contraseña que cumple la política de seguridad de CundiApp (SCRUM-66).
 *
 * <p>La política es una regla de negocio, no un formato de transporte: vive en el dominio y no en el
 * DTO, para que el registro y el restablecimiento (SCRUM-68) exijan exactamente lo mismo sin repetirla.
 *
 * <p>Solo se valida al crear o cambiar la contraseña. Al iniciar sesión nunca se aplica: las cuentas
 * creadas antes de esta política siguen entrando con lo que ya tenían.
 */
public record Contrasena(String valor) {

	public static final int LONGITUD_MINIMA = 10;

	/**
	 * bcrypt solo tiene en cuenta los primeros 72 bytes. Aceptar más daría una falsa sensación de
	 * seguridad: dos contraseñas larguísimas que coincidan en ese prefijo abrirían la misma cuenta.
	 */
	public static final int MAXIMO_BYTES = 72;

	public Contrasena {
		if (valor == null || valor.isBlank()) {
			throw new ReglaDeNegocioVioladaException("La contraseña es obligatoria");
		}
		List<String> faltantes = requisitosQueFaltan(valor);
		if (!faltantes.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("La contraseña debe tener " + enumerar(faltantes));
		}
		if (valor.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
			throw new ReglaDeNegocioVioladaException(
					"La contraseña no puede superar los " + MAXIMO_BYTES + " caracteres");
		}
	}

	/**
	 * Contraseña nueva para una cuenta. Además de la política, rechaza la que contenga el usuario del
	 * correo: es lo primero que prueba quien quiere entrar a una cuenta ajena.
	 */
	public static Contrasena nueva(String valor, CorreoInstitucional correo) {
		Contrasena contrasena = new Contrasena(valor);
		String usuario = correo.valor().split("@")[0].toLowerCase();
		if (usuario.length() >= 4 && valor.toLowerCase().contains(usuario)) {
			throw new ReglaDeNegocioVioladaException(
					"La contraseña no puede contener tu usuario del correo institucional");
		}
		return contrasena;
	}

	/** Qué le falta a una contraseña para cumplir la política, en el orden en que se le muestra a la persona. */
	public static List<String> requisitosQueFaltan(String valor) {
		List<String> faltantes = new ArrayList<>();
		if (valor == null || valor.length() < LONGITUD_MINIMA) {
			faltantes.add("al menos " + LONGITUD_MINIMA + " caracteres");
		}
		if (valor == null || valor.chars().noneMatch(Character::isUpperCase)) {
			faltantes.add("una mayúscula");
		}
		if (valor == null || valor.chars().noneMatch(Character::isLowerCase)) {
			faltantes.add("una minúscula");
		}
		if (valor == null || valor.chars().noneMatch(Character::isDigit)) {
			faltantes.add("un número");
		}
		if (valor == null || valor.chars().noneMatch(Contrasena::esEspecial)) {
			faltantes.add("un carácter especial");
		}
		return faltantes;
	}

	/** Cualquier cosa que no sea letra, número ni espacio cuenta como carácter especial. */
	private static boolean esEspecial(int caracter) {
		return !Character.isLetterOrDigit(caracter) && !Character.isWhitespace(caracter);
	}

	private static String enumerar(List<String> partes) {
		if (partes.size() == 1) {
			return partes.get(0);
		}
		return String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.get(partes.size() - 1);
	}

	/** Nunca se escribe el valor: un log o un mensaje de error no deben revelar la contraseña. */
	@Override
	public String toString() {
		return "Contrasena[oculta]";
	}
}
