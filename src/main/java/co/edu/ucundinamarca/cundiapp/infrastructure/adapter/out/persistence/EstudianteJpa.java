package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface EstudianteJpa extends JpaRepository<EstudianteEntidad, Integer> {

	boolean existsByCorreoInstitucionalIgnoreCase(String correoInstitucional);

	Optional<EstudianteEntidad> findByCorreoInstitucionalIgnoreCase(String correoInstitucional);
}
