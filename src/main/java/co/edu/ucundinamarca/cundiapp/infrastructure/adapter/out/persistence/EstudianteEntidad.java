package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
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

	/** El plan que el estudiante eligió (RF02). Nulo mientras no elija programa. */
	@Column(name = "codigo_plan")
	private String codigoPlan;

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

	String codigoPlan() {
		return codigoPlan;
	}

	void elegirPlan(String codigoPlan) {
		this.codigoPlan = codigoPlan;
	}

	void activar() {
		this.estadoCuenta = EstadoCuenta.ACTIVA.name().toLowerCase();
	}

	void desactivar() {
		this.estadoCuenta = EstadoCuenta.INACTIVA.name().toLowerCase();
	}

	/** Registrarse de nuevo con el mismo correo de una cuenta eliminada: se reescribe, no se duplica. */
	void reactivarParaRegistro(String nombres, String apellidos, boolean consentimientoDatos, Instant fechaConsentimiento) {
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.consentimientoDatos = consentimientoDatos;
		this.fechaConsentimiento = fechaConsentimiento;
		this.estadoCuenta = EstadoCuenta.PENDIENTE.name().toLowerCase();
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
