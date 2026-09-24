package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface EstudianteJpa extends JpaRepository<EstudianteEntidad, Integer> {

	boolean existsByCorreoInstitucionalIgnoreCase(String correoInstitucional);

	Optional<EstudianteEntidad> findByCorreoInstitucionalIgnoreCase(String correoInstitucional);
}
