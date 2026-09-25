package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.VerificarCorreo;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;

public class VerificarCorreoServicio implements VerificarCorreo {

	private static final String MENSAJE_GENERICO = "El código es incorrecto o venció";

	private final EstudianteRepositorio estudiantes;
	private final CodigoDeVerificacionRepositorio codigos;
	private final RelojPort reloj;

	public VerificarCorreoServicio(
			EstudianteRepositorio estudiantes, CodigoDeVerificacionRepositorio codigos, RelojPort reloj) {
		this.estudiantes = estudiantes;
		this.codigos = codigos;
		this.reloj = reloj;
	}

	@Override
	public Estudiante ejecutar(String correo, String codigo) {
		Estudiante estudiante = estudiantes.buscarPorCorreo(new CorreoInstitucional(correo))
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(MENSAJE_GENERICO));
		if (!estudiante.estaPendiente()) {
			throw new ReglaDeNegocioVioladaException("Esta cuenta ya está verificada");
		}
		CodigoDeVerificacion vigente = codigos.buscarDe(estudiante.id())
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(MENSAJE_GENERICO));

		var intento = vigente.intentar(codigo, reloj.ahora());
		// El intento se guarda antes de responder: un fallo cuenta aunque termine en error.
		codigos.guardar(intento.codigo());

		return switch (intento.resultado()) {
			case ACEPTADO -> {
				Estudiante activado = estudiante.activar();
				estudiantes.guardarActivacion(activado);
				yield activado;
			}
			case INCORRECTO -> throw new ReglaDeNegocioVioladaException(
					"El código es incorrecto. Te quedan " + intento.codigo().intentosRestantes() + " intentos");
			case VENCIDO -> throw new ReglaDeNegocioVioladaException("El código venció. Pide uno nuevo");
			case INTENTOS_AGOTADOS -> throw new ReglaDeNegocioVioladaException(
					"Agotaste los intentos. Pide un código nuevo");
			case YA_USADO -> throw new ReglaDeNegocioVioladaException(MENSAJE_GENERICO);
		};
	}
}
