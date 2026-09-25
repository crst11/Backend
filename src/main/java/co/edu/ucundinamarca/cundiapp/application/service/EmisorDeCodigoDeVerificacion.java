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
		// Se envía antes de guardar: si el envío falla no queda un código vigente que bloquee pedir otro.
		enviador.enviar(estudiante.correo(), codigo);
		repositorio.guardar(CodigoDeVerificacion.emitir(estudiante.id(), codigo, reloj.ahora()));
	}
}
