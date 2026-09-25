package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeRecurso;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class CategoriaDeRecursoAdaptador implements CategoriaDeRecursoRepositorio {

	private final CategoriaDeRecursoJpa jpa;

	CategoriaDeRecursoAdaptador(CategoriaDeRecursoJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public List<CategoriaDeRecurso> buscarTodas() {
		return jpa.findAllByOrderByOrdenAsc().stream().map(CategoriaDeRecursoEntidad::aDominio).toList();
	}
}
