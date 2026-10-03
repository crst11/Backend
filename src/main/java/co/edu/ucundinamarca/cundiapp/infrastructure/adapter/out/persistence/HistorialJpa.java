package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MatriculaAsignaturaJpa extends JpaRepository<MatriculaAsignaturaEntidad, Integer> {

	// Ordenadas por período para que el historial llegue en el orden en que se cursó.
	List<MatriculaAsignaturaEntidad> findByIdEstudianteOrderByCodigoPeriodoAscCodigoAsignaturaAsc(int idEstudiante);
}

interface ResumenPeriodoJpa extends JpaRepository<ResumenPeriodoEntidad, ResumenPeriodoEntidad.Id> {

	@Query("""
			select r from ResumenPeriodoEntidad r
			 where r.id.idEstudiante = :idEstudiante
			 order by r.id.codigoPeriodo""")
	List<ResumenPeriodoEntidad> de(@Param("idEstudiante") int idEstudiante);
}
