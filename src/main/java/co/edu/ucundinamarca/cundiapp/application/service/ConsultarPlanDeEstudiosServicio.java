package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarPlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;

public class ConsultarPlanDeEstudiosServicio implements ConsultarPlanDeEstudios {

	static final String SIN_RUTA = "Ese programa no tiene una ruta de aprendizaje cargada";

	private final ProgramaAcademicoRepositorio programas;

	public ConsultarPlanDeEstudiosServicio(ProgramaAcademicoRepositorio programas) {
		this.programas = programas;
	}

	@Override
	public PlanDeEstudios ejecutar(String codigoPrograma) {
		return programas.buscarPlanVigenteDe(codigoPrograma)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(SIN_RUTA));
	}
}
