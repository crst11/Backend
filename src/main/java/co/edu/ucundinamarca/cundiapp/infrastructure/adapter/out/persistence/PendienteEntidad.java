package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDePendiente;
import co.edu.ucundinamarca.cundiapp.domain.model.Pendiente;
import co.edu.ucundinamarca.cundiapp.domain.model.PrioridadDePendiente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * La tabla pendiente (RF06). No se mapean consec_categoria ni consec_actividad: atar un pendiente a una
 * actividad evaluativa llega con las notificaciones (RF10) y mientras tanto quedan en NULL.
 */
@Entity
@Table(name = "pendiente", schema = "cundiapp")
class PendienteEntidad {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pendiente")
	private Integer id;

	@Column(name = "id_estudiante", nullable = false)
	private int idEstudiante;

	@Column(name = "id_matricula")
	private Integer idMatricula;

	@Column(name = "titulo", nullable = false)
	private String titulo;

	@Column(name = "descripcion")
	private String descripcion;

	@Column(name = "fecha_limite", nullable = false)
	private Instant fechaLimite;

	@Column(name = "prioridad", nullable = false)
	private String prioridad;

	@Column(name = "estado_pendiente", nullable = false)
	private String estado;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion;

	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected PendienteEntidad() {
	}

	static PendienteEntidad nueva(Pendiente pendiente) {
		PendienteEntidad entidad = new PendienteEntidad();
		entidad.idEstudiante = pendiente.idEstudiante();
		entidad.fechaCreacion = pendiente.fechaCreacion();
		entidad.aplicar(pendiente, pendiente.fechaCreacion());
		return entidad;
	}

	/** Lo que puede cambiar de un pendiente que ya existe: se escribe todo y se marca cuándo. */
	void aplicar(Pendiente pendiente, Instant ahora) {
		idMatricula = pendiente.idMatricula();
		titulo = pendiente.titulo();
		descripcion = pendiente.descripcion();
		fechaLimite = pendiente.fechaLimite();
		prioridad = pendiente.prioridad().valorEnBd();
		estado = pendiente.estado().valorEnBd();
		actualizadoEn = ahora;
	}

	Pendiente aDominio() {
		return new Pendiente(id, idEstudiante, idMatricula, titulo, descripcion, fechaLimite,
				PrioridadDePendiente.desdeBd(prioridad), EstadoDePendiente.desdeBd(estado), fechaCreacion);
	}
}
