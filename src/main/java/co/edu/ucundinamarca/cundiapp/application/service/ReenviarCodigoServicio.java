package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;

public class ReenviarCodigoServicio implements ReenviarCodigoDeVerificacion {

	private final EstudianteRepositorio estudiantes;
	private final CodigoDeVerificacionRepositorio codigos;
	private final EmisorDeCodigoDeVerificacion emisor;
	private final RelojPort reloj;

	public ReenviarCodigoServicio(
			EstudianteRepositorio estudiantes,
			CodigoDeVerificacionRepositorio codigos,
			EmisorDeCodigoDeVerificacion emisor,
			RelojPort reloj) {
		this.estudiantes = estudiantes;
		this.codigos = codigos;
		this.emisor = emisor;
		this.reloj = reloj;
	}

	@Override
	public void ejecutar(String correo) {
		// Si no hay una cuenta pendiente con ese correo no se hace nada ni se avisa: no se revela quién tiene cuenta.
		estudiantes.buscarPorCorreo(new CorreoInstitucional(correo))
				.filter(estudiante -> estudiante.estaPendiente())
				.ifPresent(estudiante -> {
					boolean puedeReemitir = codigos.buscarDe(estudiante.id())
							.map(codigo -> codigo.puedeReemitirse(reloj.ahora()))
							.orElse(true);
					if (!puedeReemitir) {
						throw new ReglaDeNegocioVioladaException(
								"Tu código actual sigue vigente. Revisa tu correo o espera a que venza");
					}
					emisor.emitirYEnviar(estudiante);
				});
	}
}
