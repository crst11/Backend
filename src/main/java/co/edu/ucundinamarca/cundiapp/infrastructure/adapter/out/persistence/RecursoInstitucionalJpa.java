package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RecursoInstitucionalJpa extends JpaRepository<RecursoInstitucionalEntidad, Integer> {

	/**
	 * Una sola consulta trae cada recurso con su categoría (join fetch), sin una consulta extra por fila.
	 * Dentro de cada categoría van en el orden en que se cargaron: lo más consultado primero (V4).
	 */
	@Query("""
			SELECT r FROM RecursoInstitucionalEntidad r JOIN FETCH r.categoria c
			 WHERE r.estado <> 'retirado' AND r.requiereAutenticacion = false
			 ORDER BY c.orden, r.id""")
	List<RecursoInstitucionalEntidad> listarPublicados();
}
