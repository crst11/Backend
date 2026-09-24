package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CodigoDeVerificacion;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class CodigoDeVerificacionAdaptador implements CodigoDeVerificacionRepositorio {

	private final CodigoDeVerificacionJpa jpa;

	CodigoDeVerificacionAdaptador(CodigoDeVerificacionJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public Optional<CodigoDeVerificacion> buscarDe(int idEstudiante) {
		return jpa.findById(idEstudiante).map(CodigoDeVerificacionEntidad::aDominio);
	}

	@Override
	public void guardar(CodigoDeVerificacion codigo) {
		jpa.save(CodigoDeVerificacionEntidad.desde(codigo));
	}
}
