package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.IniciarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.SesionIniciada;
import co.edu.ucundinamarca.cundiapp.application.port.out.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDeIntentosPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.CredencialesInvalidasException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadosIntentosException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MetodoDeAcceso;
import java.time.Instant;
import java.util.Optional;

public class IniciarSesionServicio implements IniciarSesion {

	private final EstudianteRepositorio estudiantes;
	private final CifradorDeContrasenaPort cifrador;
	private final LimitadorDeIntentosPort limitador;
	private final AbridorDeSesion abridor;
	private final RelojPort reloj;

	public IniciarSesionServicio(
			EstudianteRepositorio estudiantes,
			CifradorDeContrasenaPort cifrador,
			LimitadorDeIntentosPort limitador,
			AbridorDeSesion abridor,
			RelojPort reloj) {
		this.estudiantes = estudiantes;
		this.cifrador = cifrador;
		this.limitador = limitador;
		this.abridor = abridor;
		this.reloj = reloj;
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
		SesionIniciada sesion = abridor.abrir(cuenta, MetodoDeAcceso.LOCAL, origen, ahora);
		estudiantes.registrarUltimoAcceso(cuenta.id(), ahora);
		return sesion;
	}

	private Optional<Estudiante> buscar(String correo) {
		try {
			return estudiantes.buscarPorCorreo(new CorreoInstitucional(correo));
		} catch (ReglaDeNegocioVioladaException e) {
			// Un correo que ni siquiera es institucional recibe la misma respuesta que uno inexistente.
			return Optional.empty();
		}
	}
}
