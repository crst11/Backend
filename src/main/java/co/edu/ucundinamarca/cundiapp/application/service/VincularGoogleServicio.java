package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.VincularGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VinculoConGoogleRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.GoogleYaVinculadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;
import java.util.Optional;

public class VincularGoogleServicio implements VincularGoogle {

	private final EstudianteRepositorio estudiantes;
	private final VerificadorDeIdentidadExternaPort verificador;
	private final VinculoConGoogleRepositorio vinculos;
	private final RelojPort reloj;

	public VincularGoogleServicio(
			EstudianteRepositorio estudiantes,
			VerificadorDeIdentidadExternaPort verificador,
			VinculoConGoogleRepositorio vinculos,
			RelojPort reloj) {
		this.estudiantes = estudiantes;
		this.verificador = verificador;
		this.vinculos = vinculos;
		this.reloj = reloj;
	}

	@Override
	public VinculoConGoogle ejecutar(int idEstudiante, String idToken) {
		Estudiante cuenta = estudiantes.buscarPorId(idEstudiante).orElseThrow(SesionInvalidaException::new);
		if (cuenta.estado() != EstadoCuenta.ACTIVA) {
			throw new ReglaDeNegocioVioladaException("Verifica tu correo institucional antes de vincular Google");
		}

		var identidad = verificador.verificar(idToken);
		if (!identidad.correoVerificado()) {
			throw new ReglaDeNegocioVioladaException("Google no ha verificado el correo de esa cuenta. Usa otra cuenta de Google");
		}
		Optional<Integer> duenio = vinculos.estudianteVinculadoA(identidad.identificador());
		if (duenio.isPresent() && duenio.get() != idEstudiante) {
			throw new GoogleYaVinculadoException("Esa cuenta de Google ya está vinculada a otra cuenta de CundiApp");
		}

		Optional<VinculoConGoogle> actual = vinculos.buscarDe(idEstudiante);
		if (actual.isPresent()) {
			if (actual.get().esDe(identidad.identificador())) {
				return actual.get();
			}
			throw new GoogleYaVinculadoException("Ya tienes otra cuenta de Google vinculada. Desvincúlala primero");
		}

		var nuevo = new VinculoConGoogle(identidad.identificador(), identidad.correo(), reloj.ahora());
		vinculos.vincular(idEstudiante, nuevo);
		return nuevo;
	}
}
