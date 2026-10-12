package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class EstudianteAdaptador implements EstudianteRepositorio {

	private final EstudianteJpa estudianteJpa;
	private final CredencialAccesoJpa credencialJpa;

	EstudianteAdaptador(EstudianteJpa estudianteJpa, CredencialAccesoJpa credencialJpa) {
		this.estudianteJpa = estudianteJpa;
		this.credencialJpa = credencialJpa;
	}

	@Override
	public Optional<Estudiante> buscarPorCorreo(CorreoInstitucional correo) {
		return estudianteJpa.findByCorreoInstitucionalIgnoreCase(correo.valor()).map(EstudianteEntidad::aDominio);
	}

	@Override
	public Optional<Estudiante> buscarPorId(int idEstudiante) {
		return estudianteJpa.findById(idEstudiante).map(EstudianteEntidad::aDominio);
	}

	@Override
	public Optional<String> contrasenaCifradaDe(int idEstudiante) {
		return credencialJpa.findById(new CredencialAccesoId(idEstudiante, "local"))
				.filter(CredencialAccesoEntidad::estaActiva)
				.map(CredencialAccesoEntidad::getHashContrasena);
	}

	@Override
	@Transactional
	public void registrarUltimoAcceso(int idEstudiante, Instant fecha) {
		credencialJpa.findById(new CredencialAccesoId(idEstudiante, "local")).orElseThrow().registrarAcceso(fecha);
	}

	@Override
	@Transactional
	public void guardarActivacion(Estudiante activado) {
		estudianteJpa.findById(activado.id()).orElseThrow().activar();
		credencialJpa.findById(new CredencialAccesoId(activado.id(), "local")).orElseThrow().marcarCorreoVerificado();
	}

	@Override
	public Optional<String> planDe(int idEstudiante) {
		return estudianteJpa.findById(idEstudiante).map(EstudianteEntidad::codigoPlan);
	}

	@Override
	@Transactional
	public void asignarPlan(int idEstudiante, String codigoPlan) {
		estudianteJpa.findById(idEstudiante).orElseThrow().elegirPlan(codigoPlan);
	}

	@Override
	@Transactional
	public void cambiarContrasenaLocal(int idEstudiante, String hashContrasena) {
		credencialJpa.findById(new CredencialAccesoId(idEstudiante, "local")).orElseThrow()
				.cambiarContrasena(hashContrasena);
	}

	@Override
	@Transactional
	public void guardarDesactivacion(Estudiante desactivado) {
		estudianteJpa.findById(desactivado.id()).orElseThrow().desactivar();
	}

	@Override
	@Transactional
	public Estudiante guardarConCredencialLocal(Estudiante estudiante, String hashContrasena) {
		EstudianteEntidad guardado = estudianteJpa.save(
				EstudianteEntidad.desde(estudiante, estudiante.fechaConsentimiento()));
		credencialJpa.save(CredencialAccesoEntidad.local(
				guardado.getId(), estudiante.correo().valor(), hashContrasena, estudiante.fechaConsentimiento()));
		return guardado.aDominio();
	}

	@Override
	@Transactional
	public Estudiante reactivarConCredencialLocal(Estudiante estudiante, String hashContrasena) {
		EstudianteEntidad entidad = estudianteJpa.findById(estudiante.id()).orElseThrow();
		entidad.reactivarParaRegistro(
				estudiante.nombres(), estudiante.apellidos(), estudiante.consentimientoDatos(), estudiante.fechaConsentimiento());
		credencialJpa.findById(new CredencialAccesoId(estudiante.id(), "local")).orElseThrow()
				.reiniciarParaRegistro(hashContrasena, estudiante.fechaConsentimiento());
		return entidad.aDominio();
	}
}
