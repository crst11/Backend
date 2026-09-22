package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CategoriaDeRecurso;
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
