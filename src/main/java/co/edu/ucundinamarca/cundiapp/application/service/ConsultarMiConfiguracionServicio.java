package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion;
import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;

/** Los ajustes del estudiante tal como están guardados (RF08, SCRUM-34). */
public class ConsultarMiConfiguracionServicio implements ConsultarMiConfiguracion {

	private final ConfiguracionRepositorio configuracion;

	public ConsultarMiConfiguracionServicio(ConfiguracionRepositorio configuracion) {
		this.configuracion = configuracion;
	}

	@Override
	public Configuracion ejecutar(int idEstudiante) {
		return new Configuracion(configuracion.metaDe(idEstudiante));
	}
}
