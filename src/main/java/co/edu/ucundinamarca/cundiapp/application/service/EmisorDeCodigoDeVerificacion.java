package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.GeneradorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;

/**
 * Genera un código, lo envía al correo del estudiante y guarda su huella. Lo usan el registro, el
 * reenvío y la recuperación de la contraseña; el propósito distingue para qué sirve cada uno.
 */
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

	public void emitirYEnviar(Estudiante estudiante, PropositoDelCodigo proposito) {
		String codigo = generador.generar();
		// Se guarda antes de enviar: el envío ocurre fuera de la petición (SCRUM-67) y ya no puede
		// avisar su falla a tiempo. Si el correo no sale, se puede pedir otro al minuto.
		repositorio.guardar(CodigoDeVerificacion.emitir(estudiante.id(), proposito, codigo, reloj.ahora()));
		enviador.enviar(estudiante.correo(), codigo, proposito);
	}
}
