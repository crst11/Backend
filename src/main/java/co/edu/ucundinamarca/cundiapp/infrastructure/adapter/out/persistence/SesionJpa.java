package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SesionJpa extends JpaRepository<SesionEntidad, SesionId> {

	Optional<SesionEntidad> findByHuellaRefresco(String huellaRefresco);

	@Query("select coalesce(max(s.id.consecSesion), 0) from SesionEntidad s where s.id.idEstudiante = :idEstudiante")
	int ultimoConsecutivo(@Param("idEstudiante") int idEstudiante);

	// Serializa las aperturas de sesión de un mismo estudiante para que el consecutivo no se repita.
	@Query(value = "select 1 from (select pg_advisory_xact_lock(:idEstudiante)) bloqueo", nativeQuery = true)
	int bloquear(@Param("idEstudiante") long idEstudiante);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update SesionEntidad s set s.fechaRevocacion = :ahora, s.motivoRevocacion = :motivo
			 where s.id.idEstudiante = :idEstudiante and s.fechaRevocacion is null""")
	int revocarVigentes(
			@Param("idEstudiante") int idEstudiante, @Param("ahora") Instant ahora, @Param("motivo") String motivo);
}
