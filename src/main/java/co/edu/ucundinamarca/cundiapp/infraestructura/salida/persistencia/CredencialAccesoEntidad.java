package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "credencial_acceso", schema = "cundiapp")
class CredencialAccesoEntidad {

	@EmbeddedId
	private CredencialAccesoId id;

	@Column(name = "hash_contrasena")
	private String hashContrasena;

	@Column(name = "correo_proveedor", nullable = false)
	private String correoProveedor;

	@Column(name = "correo_verificado", nullable = false)
	private boolean correoVerificado;

	@Column(name = "fecha_vinculacion", nullable = false)
	private Instant fechaVinculacion;

	@Column(name = "fecha_ultimo_acceso")
	private Instant fechaUltimoAcceso;

	@Column(name = "activa", nullable = false)
	private boolean activa;

	protected CredencialAccesoEntidad() {
	}

	void marcarCorreoVerificado() {
		this.correoVerificado = true;
	}

	void registrarAcceso(Instant fecha) {
		this.fechaUltimoAcceso = fecha;
	}

	boolean estaActiva() {
		return activa;
	}

	String getHashContrasena() {
		return hashContrasena;
	}

	static CredencialAccesoEntidad local(Integer idEstudiante, String correo, String hashContrasena, Instant ahora) {
		CredencialAccesoEntidad entidad = new CredencialAccesoEntidad();
		entidad.id = new CredencialAccesoId(idEstudiante, "local");
		entidad.hashContrasena = hashContrasena;
		entidad.correoProveedor = correo;
		entidad.correoVerificado = false;
		entidad.fechaVinculacion = ahora;
		entidad.activa = true;
		return entidad;
	}
}
