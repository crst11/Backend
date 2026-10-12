package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.DatosDeRestablecimiento;
import co.edu.ucundinamarca.cundiapp.application.port.in.RestablecerContrasena;
import co.edu.ucundinamarca.cundiapp.application.port.out.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Contrasena;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;

/**
 * Define una contraseña nueva con el código que llegó al correo (SCRUM-68).
 *
 * <p>Al terminar se revocan todas las sesiones abiertas. Quien recupera su contraseña suele hacerlo
 * porque sospecha que alguien más entró: dejar esas sesiones vivas haría inútil el cambio.
 */
public class RestablecerContrasenaServicio implements RestablecerContrasena {

	private static final String RECHAZO = "El código no es válido o ya venció. Pide uno nuevo";

	private final EstudianteRepositorio estudiantes;
	private final CodigoDeVerificacionRepositorio codigos;
	private final SesionRepositorio sesiones;
	private final CifradorDeContrasenaPort cifrador;
	private final RelojPort reloj;

	public RestablecerContrasenaServicio(
			EstudianteRepositorio estudiantes,
			CodigoDeVerificacionRepositorio codigos,
			SesionRepositorio sesiones,
			CifradorDeContrasenaPort cifrador,
			RelojPort reloj) {
		this.estudiantes = estudiantes;
		this.codigos = codigos;
		this.sesiones = sesiones;
		this.cifrador = cifrador;
		this.reloj = reloj;
	}

	@Override
	public void ejecutar(DatosDeRestablecimiento datos) {
		CorreoInstitucional correo = new CorreoInstitucional(datos.correo());
		// La política se valida antes de mirar el código: una contraseña que no cumple se rechaza igual.
		Contrasena contrasenaNueva = Contrasena.nueva(datos.contrasenaNuevaSinCifrar(), correo);

		Estudiante estudiante = estudiantes.buscarPorCorreo(correo)
				.filter(Estudiante::estaActiva)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(RECHAZO));
		String hashActual = estudiantes.contrasenaCifradaDe(estudiante.id())
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(RECHAZO));

		CodigoDeVerificacion vigente = codigos.buscarDe(estudiante.id(), PropositoDelCodigo.RECUPERAR_CONTRASENA)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(RECHAZO));
		CodigoDeVerificacion.Intento intento = vigente.intentar(datos.codigo(), reloj.ahora());
		codigos.guardar(intento.codigo());
		if (intento.resultado() != CodigoDeVerificacion.Resultado.ACEPTADO) {
			throw new ReglaDeNegocioVioladaException(mensajeDe(intento));
		}

		if (cifrador.coincide(contrasenaNueva.valor(), hashActual)) {
			throw new ReglaDeNegocioVioladaException("La contraseña nueva debe ser distinta de la anterior");
		}

		estudiantes.cambiarContrasenaLocal(estudiante.id(), cifrador.cifrar(contrasenaNueva.valor()));
		// Se reutiliza CIERRE_SESION, como ya hace eliminar cuenta: el esquema solo admite esos cuatro
		// motivos y agregar uno propio sería un cambio de base de datos que esta historia no necesita.
		sesiones.revocarVigentes(estudiante.id(), MotivoDeRevocacion.CIERRE_SESION, reloj.ahora());
	}

	private static String mensajeDe(CodigoDeVerificacion.Intento intento) {
		return switch (intento.resultado()) {
			case INCORRECTO -> "El código es incorrecto. Te quedan " + intento.codigo().intentosRestantes() + " intentos";
			case INTENTOS_AGOTADOS -> "Agotaste los intentos. Pide un código nuevo";
			default -> RECHAZO;
		};
	}
}
