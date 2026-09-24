package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;

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
