package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Instant;

/** La cuenta del estudiante. Nace en estado PENDIENTE hasta que se verifica el correo (RF01). */
public record Estudiante(
		Integer id,
		String nombres,
		String apellidos,
		CorreoInstitucional correo,
		EstadoCuenta estado,
		boolean consentimientoDatos,
		Instant fechaConsentimiento) {

	public Estudiante {
		if (nombres == null || nombres.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El nombre es obligatorio");
		}
		if (apellidos == null || apellidos.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El apellido es obligatorio");
		}
		if (correo == null) {
			throw new ReglaDeNegocioVioladaException("El correo institucional es obligatorio");
		}
		// Ley 1581 de 2012: no se registra sin aceptar el tratamiento de datos, y queda la fecha.
		if (!consentimientoDatos) {
			throw new ReglaDeNegocioVioladaException("Debes aceptar el tratamiento de datos para registrarte");
		}
		if (fechaConsentimiento == null) {
			throw new ReglaDeNegocioVioladaException("Falta la fecha del consentimiento");
		}
	}

	public boolean estaPendiente() {
		return estado == EstadoCuenta.PENDIENTE;
	}

	public boolean estaActiva() {
		return estado == EstadoCuenta.ACTIVA;
	}

	/** La cuenta se activa solo al verificar el correo: una cuenta inactiva no vuelve por esta vía. */
	public Estudiante activar() {
		if (estado == EstadoCuenta.INACTIVA) {
			throw new ReglaDeNegocioVioladaException("Una cuenta inactiva no se puede activar con un código");
		}
		return new Estudiante(id, nombres, apellidos, correo, EstadoCuenta.ACTIVA, consentimientoDatos, fechaConsentimiento);
	}

	/**
	 * Elimina la cuenta sin borrar el registro (RF01): queda inactiva, no se puede volver a
	 * activar con un código y el inicio de sesión la rechaza, con o sin Google.
	 */
	public Estudiante desactivar() {
		if (estado == EstadoCuenta.INACTIVA) {
			throw new ReglaDeNegocioVioladaException("La cuenta ya está inactiva");
		}
		return new Estudiante(id, nombres, apellidos, correo, EstadoCuenta.INACTIVA, consentimientoDatos, fechaConsentimiento);
	}
}
