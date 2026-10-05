package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class SesionAdaptador implements SesionRepositorio {

	private static final Logger log = LoggerFactory.getLogger(SesionAdaptador.class);

	private final SesionJpa jpa;

	SesionAdaptador(SesionJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	@Transactional
	public Sesion guardar(Sesion nueva) {
		return insertar(nueva);
	}

	@Override
	public Optional<Sesion> buscarPorHuella(String huellaRefresco) {
		return jpa.findByHuellaRefresco(huellaRefresco).map(SesionEntidad::aDominio);
	}

	@Override
	@Transactional
	public Sesion rotar(Sesion revocada, Sesion nueva) {
		actualizar(revocada);
		return insertar(nueva);
	}

	@Override
	@Transactional
	public void actualizar(Sesion sesion) {
		SesionEntidad entidad = jpa.findById(new SesionId(sesion.idEstudiante(), sesion.consecutivo())).orElseThrow();
		entidad.aplicarRevocacion(sesion.fechaRevocacion(), sesion.motivoRevocacion());
	}

	@Override
	public List<Sesion> listarVigentes(int idEstudiante, Instant ahora) {
		return jpa.vigentes(idEstudiante, ahora).stream().map(SesionEntidad::aDominio).toList();
	}

	@Override
	@Transactional
	public boolean revocarUna(int idEstudiante, int consecutivo, MotivoDeRevocacion motivo, Instant ahora) {
		return jpa.revocarUna(idEstudiante, consecutivo, ahora, motivo.valorEnBd()) == 1;
	}

	@Override
	@Transactional
	public void revocarVigentes(int idEstudiante, MotivoDeRevocacion motivo, Instant ahora) {
		int revocadas = jpa.revocarVigentes(idEstudiante, ahora, motivo.valorEnBd());
		if (motivo == MotivoDeRevocacion.REUSO_DETECTADO) {
			// Evento de seguridad: alguien usó un token de refresco que ya se había cambiado por otro.
			log.warn("Reutilización de un token de refresco: se revocaron {} sesiones del estudiante {}", revocadas, idEstudiante);
		}
	}

	/** Devuelve la sesión ya con su consecutivo: el token de acceso lo lleva como «sid» (SCRUM-73). */
	private Sesion insertar(Sesion sesion) {
		jpa.bloquear(sesion.idEstudiante());
		int consecutivo = jpa.ultimoConsecutivo(sesion.idEstudiante()) + 1;
		return jpa.save(SesionEntidad.nueva(sesion, consecutivo)).aDominio();
	}
}
