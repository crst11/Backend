package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
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
	public boolean existeCuentaCon(CorreoInstitucional correo) {
		return estudianteJpa.existsByCorreoInstitucionalIgnoreCase(correo.valor());
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
	@Transactional
	public Estudiante guardarConCredencialLocal(Estudiante estudiante, String hashContrasena) {
		EstudianteEntidad guardado = estudianteJpa.save(
				EstudianteEntidad.desde(estudiante, estudiante.fechaConsentimiento()));
		credencialJpa.save(CredencialAccesoEntidad.local(
				guardado.getId(), estudiante.correo().valor(), hashContrasena, estudiante.fechaConsentimiento()));
		return guardado.aDominio();
	}
}
