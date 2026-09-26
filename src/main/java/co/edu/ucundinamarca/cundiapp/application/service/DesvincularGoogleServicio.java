package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.DesvincularGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.out.VinculoConGoogleRepositorio;

public class DesvincularGoogleServicio implements DesvincularGoogle {

	private final VinculoConGoogleRepositorio vinculos;

	public DesvincularGoogleServicio(VinculoConGoogleRepositorio vinculos) {
		this.vinculos = vinculos;
	}

	@Override
	public void ejecutar(int idEstudiante) {
		vinculos.desvincular(idEstudiante);
	}
}
