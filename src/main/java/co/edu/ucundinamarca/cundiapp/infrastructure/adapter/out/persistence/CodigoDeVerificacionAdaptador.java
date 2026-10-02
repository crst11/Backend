package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class CodigoDeVerificacionAdaptador implements CodigoDeVerificacionRepositorio {

	private final CodigoDeVerificacionJpa jpa;

	CodigoDeVerificacionAdaptador(CodigoDeVerificacionJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public Optional<CodigoDeVerificacion> buscarDe(int idEstudiante, PropositoDelCodigo proposito) {
		return jpa.findById(new CodigoDeVerificacionId(idEstudiante, proposito.valorEnBaseDeDatos()))
				.map(CodigoDeVerificacionEntidad::aDominio);
	}

	@Override
	public void guardar(CodigoDeVerificacion codigo) {
		jpa.save(CodigoDeVerificacionEntidad.desde(codigo));
	}
}
