package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.RecursoInstitucionalRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.RecursoInstitucional;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class RecursoInstitucionalAdaptador implements RecursoInstitucionalRepositorio {

	private final RecursoInstitucionalJpa jpa;

	RecursoInstitucionalAdaptador(RecursoInstitucionalJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	@Transactional(readOnly = true)
	public List<RecursoInstitucional> listarPublicados() {
		return jpa.listarPublicados().stream().map(RecursoInstitucionalEntidad::aDominio).toList();
	}
}
