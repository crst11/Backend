package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoriaDeRecursoJpa extends JpaRepository<CategoriaDeRecursoEntidad, Integer> {

	List<CategoriaDeRecursoEntidad> findAllByOrderByOrdenAsc();
}
