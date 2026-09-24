package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

interface CodigoDeVerificacionJpa extends JpaRepository<CodigoDeVerificacionEntidad, Integer> {
}
