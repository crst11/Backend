package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.IniciarSesionConGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.in.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.SesionIniciada;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VinculoConGoogleRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.GoogleNoVinculadoException;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MetodoDeAcceso;
import java.time.Instant;

public class IniciarSesionConGoogleServicio implements IniciarSesionConGoogle {

	private final VerificadorDeIdentidadExternaPort verificador;
	private final VinculoConGoogleRepositorio vinculos;
	private final EstudianteRepositorio estudiantes;
	private final AbridorDeSesion abridor;
	private final RelojPort reloj;

	public IniciarSesionConGoogleServicio(
			VerificadorDeIdentidadExternaPort verificador,
			VinculoConGoogleRepositorio vinculos,
			EstudianteRepositorio estudiantes,
			AbridorDeSesion abridor,
			RelojPort reloj) {
		this.verificador = verificador;
		this.vinculos = vinculos;
		this.estudiantes = estudiantes;
		this.abridor = abridor;
		this.reloj = reloj;
	}

	@Override
	public SesionIniciada ejecutar(String idToken, OrigenDeSesion origen) {
		var identidad = verificador.verificar(idToken);
		// Solo entra quien vinculó esta cuenta de Google después de verificar su correo institucional:
		// que el correo de Google coincida con uno registrado no basta.
		int idEstudiante = vinculos.estudianteVinculadoA(identidad.identificador())
				.orElseThrow(GoogleNoVinculadoException::new);
		Estudiante cuenta = estudiantes.buscarPorId(idEstudiante).orElseThrow(GoogleNoVinculadoException::new);
		if (cuenta.estado() != EstadoCuenta.ACTIVA) {
			throw new CuentaNoActivaException("Esta cuenta está inactiva");
		}

		Instant ahora = reloj.ahora();
		SesionIniciada sesion = abridor.abrir(cuenta, MetodoDeAcceso.GOOGLE, origen, ahora);
		vinculos.registrarAcceso(idEstudiante, ahora);
		return sesion;
	}
}
