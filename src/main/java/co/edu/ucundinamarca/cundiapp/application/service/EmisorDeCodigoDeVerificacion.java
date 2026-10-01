package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.GeneradorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;

/** Genera un código, lo envía al correo del estudiante y guarda su huella. Lo usan el registro y el reenvío. */
public class EmisorDeCodigoDeVerificacion {

	private final GeneradorDeCodigoPort generador;
	private final EnviadorDeCodigoPort enviador;
	private final CodigoDeVerificacionRepositorio repositorio;
	private final RelojPort reloj;

	public EmisorDeCodigoDeVerificacion(
			GeneradorDeCodigoPort generador,
			EnviadorDeCodigoPort enviador,
			CodigoDeVerificacionRepositorio repositorio,
			RelojPort reloj) {
		this.generador = generador;
		this.enviador = enviador;
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	public void emitirYEnviar(Estudiante estudiante) {
		String codigo = generador.generar();
		// Se guarda antes de enviar: el envío ocurre fuera de la petición (SCRUM-67) y ya no puede
		// avisar su falla a tiempo. Si el correo no sale, se puede pedir otro al minuto.
		repositorio.guardar(CodigoDeVerificacion.emitir(estudiante.id(), codigo, reloj.ahora()));
		enviador.enviar(estudiante.correo(), codigo);
	}
}
