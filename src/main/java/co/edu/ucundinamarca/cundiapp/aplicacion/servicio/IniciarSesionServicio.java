package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.IniciarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.SesionIniciada;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.LimitadorDeIntentosPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.CredencialesInvalidasException;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.DemasiadosIntentosException;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Sesion;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class IniciarSesionServicio implements IniciarSesion {

	private final EstudianteRepositorio estudiantes;
	private final SesionRepositorio sesiones;
	private final CifradorDeContrasenaPort cifrador;
	private final EmisorDeTokensPort tokens;
	private final LimitadorDeIntentosPort limitador;
	private final RelojPort reloj;
	private final Duration vigenciaAcceso;
	private final Duration vigenciaRefresco;

	public IniciarSesionServicio(
			EstudianteRepositorio estudiantes,
			SesionRepositorio sesiones,
			CifradorDeContrasenaPort cifrador,
			EmisorDeTokensPort tokens,
			LimitadorDeIntentosPort limitador,
			RelojPort reloj,
			Duration vigenciaAcceso,
			Duration vigenciaRefresco) {
		this.estudiantes = estudiantes;
		this.sesiones = sesiones;
		this.cifrador = cifrador;
		this.tokens = tokens;
		this.limitador = limitador;
		this.reloj = reloj;
		this.vigenciaAcceso = vigenciaAcceso;
		this.vigenciaRefresco = vigenciaRefresco;
	}

	@Override
	public SesionIniciada ejecutar(String correo, String contrasena, OrigenDeSesion origen) {
		String claveCorreo = "correo:" + correo.trim().toLowerCase();
		String claveIp = "ip:" + origen.ip();
		if (limitador.estaBloqueado(claveCorreo) || limitador.estaBloqueado(claveIp)) {
			throw new DemasiadosIntentosException();
		}

		Optional<Estudiante> estudiante = buscar(correo);
		Optional<String> hash = estudiante.flatMap(e -> estudiantes.contrasenaCifradaDe(e.id()));
		boolean coincide;
		if (hash.isPresent()) {
			coincide = cifrador.coincide(contrasena, hash.get());
		} else {
			// Se gasta el mismo tiempo que con una cuenta real para que la demora no delate si el correo existe.
			cifrador.cifrar(contrasena);
			coincide = false;
		}
		if (!coincide) {
			limitador.registrarFallo(claveCorreo);
			limitador.registrarFallo(claveIp);
			throw new CredencialesInvalidasException();
		}

		Estudiante cuenta = estudiante.orElseThrow();
		if (cuenta.estado() == EstadoCuenta.PENDIENTE) {
			throw new CuentaNoActivaException("Verifica tu correo institucional con el código que te enviamos para poder iniciar sesión");
		}
		if (cuenta.estado() != EstadoCuenta.ACTIVA) {
			throw new CuentaNoActivaException("Esta cuenta está inactiva");
		}
		limitador.reiniciar(claveCorreo);

		Instant ahora = reloj.ahora();
		String refresco = tokens.generarTokenDeRefresco();
		Sesion sesion = Sesion.abrir(
				cuenta.id(), refresco, ahora, vigenciaRefresco, recortar(origen.userAgent(), 200), recortar(origen.ip(), 45));
		sesiones.guardar(sesion);
		estudiantes.registrarUltimoAcceso(cuenta.id(), ahora);

		var acceso = tokens.emitirAcceso(cuenta, ahora, vigenciaAcceso);
		return new SesionIniciada(cuenta, acceso.valor(), acceso.expira(), refresco, sesion.fechaExpiracion());
	}

	private Optional<Estudiante> buscar(String correo) {
		try {
			return estudiantes.buscarPorCorreo(new CorreoInstitucional(correo));
		} catch (ReglaDeNegocioVioladaException e) {
			// Un correo que ni siquiera es institucional recibe la misma respuesta que uno inexistente.
			return Optional.empty();
		}
	}

	static String recortar(String texto, int maximo) {
		if (texto == null) {
			return null;
		}
		return texto.length() <= maximo ? texto : texto.substring(0, maximo);
	}
}
