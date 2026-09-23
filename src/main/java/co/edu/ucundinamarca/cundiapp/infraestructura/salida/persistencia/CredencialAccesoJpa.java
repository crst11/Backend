package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

interface CredencialAccesoJpa extends JpaRepository<CredencialAccesoEntidad, CredencialAccesoId> {
}
