package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProgramaAcademicoJpa extends JpaRepository<ProgramaAcademicoEntidad, String> {

	// Solo los que ya tienen ruta cargada: ofrecer uno sin plan dejaría al estudiante en una pantalla vacía.
	@Query("""
			select p from ProgramaAcademicoEntidad p
			 where exists (select 1 from PlanDeEstudiosEntidad pe
			                where pe.codigoPrograma = p.codigo and pe.estado = 'vigente')
			 order by p.nombre, p.sede""")
	List<ProgramaAcademicoEntidad> conPlanVigente();
}

interface PlanDeEstudiosJpa extends JpaRepository<PlanDeEstudiosEntidad, String> {

	Optional<PlanDeEstudiosEntidad> findByCodigoProgramaAndEstado(String codigoPrograma, String estado);
}

interface AsignaturaJpa extends JpaRepository<AsignaturaEntidad, String> {

	List<AsignaturaEntidad> findByCodigoPlan(String codigoPlan);
}

interface PrerrequisitoJpa extends JpaRepository<PrerrequisitoEntidad, PrerrequisitoEntidad.Id> {

	@Query("""
			select r from PrerrequisitoEntidad r
			 where r.id.codigoAsignatura in (
			       select a.codigo from AsignaturaEntidad a where a.codigoPlan = :codigoPlan)""")
	List<PrerrequisitoEntidad> delPlan(@Param("codigoPlan") String codigoPlan);
}
