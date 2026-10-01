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
		// Se envía antes de guardar: si el envío falla no queda un código vigente que bloquee pedir otro.
		enviador.enviar(estudiante.correo(), codigo, proposito);
		repositorio.guardar(CodigoDeVerificacion.emitir(estudiante.id(), proposito, codigo, reloj.ahora()));
	}
}
