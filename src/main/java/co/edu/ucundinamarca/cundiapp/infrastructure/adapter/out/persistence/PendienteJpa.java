package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PendienteJpa extends JpaRepository<PendienteEntidad, Integer> {

	List<PendienteEntidad> findByIdEstudiante(int idEstudiante);

	/** Sin los cumplidos: lo que se ve por defecto. «vencido» no se guarda, pero si apareciera cuenta como abierto. */
	@Query("select p from PendienteEntidad p where p.idEstudiante = :idEstudiante and p.estado <> 'cumplido'")
	List<PendienteEntidad> abiertosDe(@Param("idEstudiante") int idEstudiante);

	Optional<PendienteEntidad> findByIdAndIdEstudiante(int id, int idEstudiante);

	@Modifying
	@Query("delete from PendienteEntidad p where p.id = :id and p.idEstudiante = :idEstudiante")
	int eliminar(@Param("id") int id, @Param("idEstudiante") int idEstudiante);
}
