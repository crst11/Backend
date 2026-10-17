package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiProyeccionDeNota;
import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import java.util.List;

/** La proyección con las notas reales: es una simulación sin notas hipotéticas (RF08, SCRUM-33). */
public class ConsultarMiProyeccionDeNotaServicio implements ConsultarMiProyeccionDeNota {

	private final SimularMiNota simulador;

	public ConsultarMiProyeccionDeNotaServicio(SimularMiNota simulador) {
		this.simulador = simulador;
	}

	@Override
	public SimularMiNota.Resultado ejecutar(int idEstudiante, int idMatricula, Meta meta) {
		return simulador.ejecutar(idEstudiante, idMatricula, meta, List.of());
	}
}
