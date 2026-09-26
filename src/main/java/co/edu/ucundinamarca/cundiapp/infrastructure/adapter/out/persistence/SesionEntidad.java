package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.MetodoDeAcceso;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sesion", schema = "cundiapp")
class SesionEntidad {

	@EmbeddedId
	private SesionId id;

	@Column(name = "proveedor_origen", nullable = false)
	private String proveedorOrigen;

	@Column(name = "hash_token_refresco", nullable = false)
	private String huellaRefresco;

	@Column(name = "fecha_inicio", nullable = false)
	private Instant fechaInicio;

	@Column(name = "fecha_expiracion", nullable = false)
	private Instant fechaExpiracion;

	@Column(name = "fecha_revocacion")
	private Instant fechaRevocacion;

	@Column(name = "motivo_revocacion")
	private String motivoRevocacion;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "ip_origen")
	private String ipOrigen;

	protected SesionEntidad() {
	}

	static SesionEntidad nueva(Sesion sesion, int consecutivo) {
		SesionEntidad entidad = new SesionEntidad();
		entidad.id = new SesionId(sesion.idEstudiante(), consecutivo);
		entidad.proveedorOrigen = sesion.metodo().valorEnBd();
		entidad.huellaRefresco = sesion.huellaRefresco();
		entidad.fechaInicio = sesion.fechaInicio();
		entidad.fechaExpiracion = sesion.fechaExpiracion();
		entidad.userAgent = sesion.userAgent();
		entidad.ipOrigen = sesion.ipOrigen();
		return entidad;
	}

	void aplicarRevocacion(Instant fecha, MotivoDeRevocacion motivo) {
		this.fechaRevocacion = fecha;
		this.motivoRevocacion = motivo == null ? null : motivo.valorEnBd();
	}

	Sesion aDominio() {
		return new Sesion(
				id.idEstudiante(),
				id.consecSesion(),
				MetodoDeAcceso.desdeBd(proveedorOrigen),
				huellaRefresco,
				fechaInicio,
				fechaExpiracion,
				fechaRevocacion,
				motivoRevocacion == null ? null : MotivoDeRevocacion.desdeBd(motivoRevocacion),
				userAgent,
				ipOrigen);
	}
}
