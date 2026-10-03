package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarProgramas;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import java.util.List;

public class ListarProgramasServicio implements ListarProgramas {

	private final ProgramaAcademicoRepositorio programas;

	public ListarProgramasServicio(ProgramaAcademicoRepositorio programas) {
		this.programas = programas;
	}

	@Override
	public List<ProgramaAcademico> ejecutar() {
		return programas.listarProgramas();
	}
}
