package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.MetodoDeAcceso;
import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;
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

	@Column(name = "identificador_externo")
	private String identificadorExterno;

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

	Integer idEstudiante() {
		return id.idEstudiante();
	}

	VinculoConGoogle aVinculoConGoogle() {
		return new VinculoConGoogle(identificadorExterno, correoProveedor, fechaVinculacion);
	}

	static CredencialAccesoEntidad google(Integer idEstudiante, VinculoConGoogle vinculo) {
		CredencialAccesoEntidad entidad = new CredencialAccesoEntidad();
		entidad.id = new CredencialAccesoId(idEstudiante, MetodoDeAcceso.GOOGLE.valorEnBd());
		entidad.identificadorExterno = vinculo.identificador();
		entidad.correoProveedor = vinculo.correo();
		// Solo se vincula una cuenta de Google cuyo correo ya verificó Google (VincularGoogleServicio).
		entidad.correoVerificado = true;
		entidad.fechaVinculacion = vinculo.fechaVinculacion();
		entidad.activa = true;
		return entidad;
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
