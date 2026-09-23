package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

interface EstudianteJpa extends JpaRepository<EstudianteEntidad, Integer> {

	boolean existsByCorreoInstitucionalIgnoreCase(String correoInstitucional);
}
