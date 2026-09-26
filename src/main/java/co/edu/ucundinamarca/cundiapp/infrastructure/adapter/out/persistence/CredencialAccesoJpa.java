package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CredencialAccesoJpa extends JpaRepository<CredencialAccesoEntidad, CredencialAccesoId> {

	/** El identificador externo es único en todo el esquema (uk_credencial_identificador_externo). */
	Optional<CredencialAccesoEntidad> findByIdentificadorExterno(String identificadorExterno);
}
