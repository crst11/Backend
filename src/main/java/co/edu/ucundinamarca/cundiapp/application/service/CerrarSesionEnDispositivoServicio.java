package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.CerrarSesionEnDispositivo;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;

/**
 * Cierra sesiones desde *Mis sesiones* (SCRUM-49).
 *
 * <p>Revocar es lo único que hace falta: el token de refresco deja de renovar en cuanto la sesión
 * queda revocada, así que el dispositivo pierde el acceso al vencer su token de acceso, que dura 20
 * minutos. No se revoca el token de acceso en sí porque es sin estado y no se guarda en ninguna parte.
 */
public class CerrarSesionEnDispositivoServicio implements CerrarSesionEnDispositivo {

	private final SesionRepositorio sesiones;
	private final RelojPort reloj;

	public CerrarSesionEnDispositivoServicio(SesionRepositorio sesiones, RelojPort reloj) {
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	@Override
	public void ejecutar(int idEstudiante, int consecutivo) {
		// El id del estudiante sale del token, así que nadie puede cerrar la sesión de otra persona:
		// un consecutivo ajeno sencillamente no aparece entre las suyas.
		boolean revocada = sesiones.revocarUna(idEstudiante, consecutivo, MotivoDeRevocacion.CIERRE_SESION, reloj.ahora());
		if (!revocada) {
			// 422 y no 401: un 401 haría creer que la sesión de quien pide es la que murió.
			throw new ReglaDeNegocioVioladaException("Esa sesión ya no está abierta");
		}
	}

	@Override
	public void ejecutarTodas(int idEstudiante) {
		sesiones.revocarVigentes(idEstudiante, MotivoDeRevocacion.CIERRE_SESION, reloj.ahora());
	}
}
