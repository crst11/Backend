package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisImportaciones;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.Importacion;
import java.util.List;

/** El historial de cargas del estudiante (RF03, SCRUM-24). */
public class ListarMisImportacionesServicio implements ListarMisImportaciones {

	private final ImportacionRepositorio importaciones;

	public ListarMisImportacionesServicio(ImportacionRepositorio importaciones) {
		this.importaciones = importaciones;
	}

	@Override
	public List<Importacion> ejecutar(int idEstudiante) {
		return importaciones.deEstudiante(idEstudiante);
	}
}
