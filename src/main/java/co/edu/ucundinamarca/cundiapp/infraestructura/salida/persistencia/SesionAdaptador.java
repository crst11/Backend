package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Sesion;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class SesionAdaptador implements SesionRepositorio {

	private final SesionJpa jpa;

	SesionAdaptador(SesionJpa jpa) {
		this.jpa = jpa;
	}

	@Override
	@Transactional
	public void guardar(Sesion nueva) {
		insertar(nueva);
	}

	@Override
	public Optional<Sesion> buscarPorHuella(String huellaRefresco) {
		return jpa.findByHuellaRefresco(huellaRefresco).map(SesionEntidad::aDominio);
	}

	@Override
	@Transactional
	public void rotar(Sesion revocada, Sesion nueva) {
		actualizar(revocada);
		insertar(nueva);
	}

	@Override
	@Transactional
	public void actualizar(Sesion sesion) {
		SesionEntidad entidad = jpa.findById(new SesionId(sesion.idEstudiante(), sesion.consecutivo())).orElseThrow();
		entidad.aplicarRevocacion(sesion.fechaRevocacion(), sesion.motivoRevocacion());
	}

	@Override
	@Transactional
	public void revocarVigentes(int idEstudiante, MotivoDeRevocacion motivo, Instant ahora) {
		jpa.revocarVigentes(idEstudiante, ahora, motivo.valorEnBd());
	}

	private void insertar(Sesion sesion) {
		jpa.bloquear(sesion.idEstudiante());
		int consecutivo = jpa.ultimoConsecutivo(sesion.idEstudiante()) + 1;
		jpa.save(SesionEntidad.nueva(sesion, consecutivo));
	}
}
