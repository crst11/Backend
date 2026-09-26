package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarVinculoConGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.out.VinculoConGoogleRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;
import java.util.Optional;

public class ConsultarVinculoConGoogleServicio implements ConsultarVinculoConGoogle {

	private final VinculoConGoogleRepositorio vinculos;

	public ConsultarVinculoConGoogleServicio(VinculoConGoogleRepositorio vinculos) {
		this.vinculos = vinculos;
	}

	@Override
	public Optional<VinculoConGoogle> ejecutar(int idEstudiante) {
		return vinculos.buscarDe(idEstudiante);
	}
}
