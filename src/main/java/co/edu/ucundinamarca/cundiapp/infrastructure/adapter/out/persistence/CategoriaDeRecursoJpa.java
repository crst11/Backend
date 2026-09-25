package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoriaDeRecursoJpa extends JpaRepository<CategoriaDeRecursoEntidad, Integer> {

	List<CategoriaDeRecursoEntidad> findAllByOrderByOrdenAsc();
}
