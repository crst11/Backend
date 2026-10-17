package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.PendienteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.Pendiente;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PendienteAdaptador implements PendienteRepositorio {

	private final PendienteJpa jpa;
	private final Clock reloj;

	PendienteAdaptador(PendienteJpa jpa, Clock reloj) {
		this.jpa = jpa;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public Pendiente guardar(Pendiente pendiente) {
		if (pendiente.id() == null) {
			return jpa.save(PendienteEntidad.nueva(pendiente)).aDominio();
		}
		PendienteEntidad entidad = jpa.findByIdAndIdEstudiante(pendiente.id(), pendiente.idEstudiante()).orElseThrow();
		entidad.aplicar(pendiente, reloj.instant());
		return entidad.aDominio();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Pendiente> listar(int idEstudiante, boolean incluirCumplidos) {
		List<PendienteEntidad> entidades = incluirCumplidos ? jpa.findByIdEstudiante(idEstudiante) : jpa.abiertosDe(idEstudiante);
		return entidades.stream().map(PendienteEntidad::aDominio).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Pendiente> buscar(int idEstudiante, int idPendiente) {
		return jpa.findByIdAndIdEstudiante(idPendiente, idEstudiante).map(PendienteEntidad::aDominio);
	}

	@Override
	@Transactional
	public boolean eliminar(int idEstudiante, int idPendiente) {
		return jpa.eliminar(idPendiente, idEstudiante) == 1;
	}
}
