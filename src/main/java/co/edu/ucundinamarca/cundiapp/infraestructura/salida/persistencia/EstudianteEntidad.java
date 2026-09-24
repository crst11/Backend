package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "estudiante", schema = "cundiapp")
class EstudianteEntidad {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_estudiante")
	private Integer id;

	@Column(name = "nombres", nullable = false)
	private String nombres;

	@Column(name = "apellidos", nullable = false)
	private String apellidos;

	@Column(name = "correo_institucional", nullable = false)
	private String correoInstitucional;

	@Column(name = "estado_cuenta", nullable = false)
	private String estadoCuenta;

	@Column(name = "consentimiento_datos", nullable = false)
	private boolean consentimientoDatos;

	@Column(name = "fecha_consentimiento")
	private Instant fechaConsentimiento;

	@Column(name = "fecha_registro", nullable = false)
	private Instant fechaRegistro;

	protected EstudianteEntidad() {
	}

	static EstudianteEntidad desde(Estudiante estudiante, Instant fechaRegistro) {
		EstudianteEntidad entidad = new EstudianteEntidad();
		entidad.nombres = estudiante.nombres();
		entidad.apellidos = estudiante.apellidos();
		entidad.correoInstitucional = estudiante.correo().valor();
		entidad.estadoCuenta = estudiante.estado().name().toLowerCase();
		entidad.consentimientoDatos = estudiante.consentimientoDatos();
		entidad.fechaConsentimiento = estudiante.fechaConsentimiento();
		entidad.fechaRegistro = fechaRegistro;
		return entidad;
	}

	Integer getId() {
		return id;
	}

	void activar() {
		this.estadoCuenta = EstadoCuenta.ACTIVA.name().toLowerCase();
	}

	Estudiante aDominio() {
		return new Estudiante(
				id,
				nombres,
				apellidos,
				new CorreoInstitucional(correoInstitucional),
				EstadoCuenta.valueOf(estadoCuenta.toUpperCase()),
				consentimientoDatos,
				fechaConsentimiento);
	}
}
