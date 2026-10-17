package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMisUmbrales;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion;
import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;

/** Guarda los umbrales de riesgo. Que sean válidos lo garantiza {@link UmbralesDeRiesgo} (RF09, SCRUM-36). */
public class CambiarMisUmbralesServicio implements CambiarMisUmbrales {

	private final ConfiguracionRepositorio configuracion;

	public CambiarMisUmbralesServicio(ConfiguracionRepositorio configuracion) {
		this.configuracion = configuracion;
	}

	@Override
	public ConsultarMiConfiguracion.Configuracion ejecutar(int idEstudiante, UmbralesDeRiesgo umbrales) {
		if (umbrales == null) {
			throw new ReglaDeNegocioVioladaException("Faltan los umbrales de riesgo");
		}
		configuracion.guardarUmbrales(idEstudiante, umbrales);
		return new ConsultarMiConfiguracion.Configuracion(configuracion.metaDe(idEstudiante), umbrales);
	}
}
