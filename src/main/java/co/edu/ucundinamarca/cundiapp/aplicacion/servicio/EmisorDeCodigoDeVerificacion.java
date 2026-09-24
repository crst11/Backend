package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.GeneradorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;

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
