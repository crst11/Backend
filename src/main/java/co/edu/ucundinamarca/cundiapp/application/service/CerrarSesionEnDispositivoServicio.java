package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.CerrarSesionEnDispositivo;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConSesion;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import java.time.Instant;
import java.util.List;

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

	/**
	 * Cierra el dispositivo entero, no una fila suelta (SCRUM-73). Revocar solo la última sesión de
	 * la cadena dejaría abiertas las anteriores, y el dispositivo seguiría dentro.
	 */
	@Override
	public void ejecutar(int idEstudiante, int consecutivo) {
		// El id del estudiante sale del token, así que nadie puede cerrar la sesión de otra persona:
		// un consecutivo ajeno sencillamente no aparece entre las suyas.
		Instant ahora = reloj.ahora();
		List<Integer> delDispositivo = DispositivoConSesion.agrupar(sesiones.listarVigentes(idEstudiante, ahora), null)
				.stream()
				.filter(dispositivo -> dispositivo.sesiones().contains(consecutivo))
				.findFirst()
				.map(DispositivoConSesion::sesiones)
				// 422 y no 401: un 401 haría creer que la sesión de quien pide es la que murió.
				.orElseThrow(() -> new ReglaDeNegocioVioladaException("Esa sesión ya no está abierta"));

		delDispositivo.forEach(
				una -> sesiones.revocarUna(idEstudiante, una, MotivoDeRevocacion.CIERRE_SESION, ahora));
	}

	@Override
	public void ejecutarTodas(int idEstudiante) {
		sesiones.revocarVigentes(idEstudiante, MotivoDeRevocacion.CIERRE_SESION, reloj.ahora());
	}
}
