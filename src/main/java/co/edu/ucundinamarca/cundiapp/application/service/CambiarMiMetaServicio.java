package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMiMeta;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion;
import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;

/** Guarda la meta del estudiante. El rango y los decimales los garantiza {@link Meta} (RF08, SCRUM-34). */
public class CambiarMiMetaServicio implements CambiarMiMeta {

	private final ConfiguracionRepositorio configuracion;

	public CambiarMiMetaServicio(ConfiguracionRepositorio configuracion) {
		this.configuracion = configuracion;
	}

	@Override
	public ConsultarMiConfiguracion.Configuracion ejecutar(int idEstudiante, Meta meta) {
		if (meta == null) {
			throw new ReglaDeNegocioVioladaException("Falta la meta");
		}
		configuracion.guardarMeta(idEstudiante, meta);
		return new ConsultarMiConfiguracion.Configuracion(meta);
	}
}
