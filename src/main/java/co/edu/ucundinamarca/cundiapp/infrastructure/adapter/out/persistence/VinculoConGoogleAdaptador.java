package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.VinculoConGoogleRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.MetodoDeAcceso;
import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** El vínculo con Google es la fila de credencial_acceso con proveedor 'google' del estudiante. */
@Component
class VinculoConGoogleAdaptador implements VinculoConGoogleRepositorio {

	private static final String GOOGLE = MetodoDeAcceso.GOOGLE.valorEnBd();

	private final CredencialAccesoJpa jpa;

	VinculoConGoogleAdaptador(CredencialAccesoJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	public Optional<Integer> estudianteVinculadoA(String identificador) {
		return jpa.findByIdentificadorExterno(identificador)
				.filter(CredencialAccesoEntidad::estaActiva)
				.map(CredencialAccesoEntidad::idEstudiante);
	}

	@Override
	public Optional<VinculoConGoogle> buscarDe(int idEstudiante) {
		return jpa.findById(new CredencialAccesoId(idEstudiante, GOOGLE))
				.filter(CredencialAccesoEntidad::estaActiva)
				.map(CredencialAccesoEntidad::aVinculoConGoogle);
	}

	@Override
	@Transactional
	public void vincular(int idEstudiante, VinculoConGoogle vinculo) {
		jpa.save(CredencialAccesoEntidad.google(idEstudiante, vinculo));
	}

	@Override
	@Transactional
	public void desvincular(int idEstudiante) {
		// Se borra la fila: así esa cuenta de Google puede vincularse después a otra cuenta si hace falta.
		jpa.deleteById(new CredencialAccesoId(idEstudiante, GOOGLE));
	}

	@Override
	@Transactional
	public void registrarAcceso(int idEstudiante, Instant fecha) {
		jpa.findById(new CredencialAccesoId(idEstudiante, GOOGLE)).ifPresent(credencial -> credencial.registrarAcceso(fecha));
	}
}
