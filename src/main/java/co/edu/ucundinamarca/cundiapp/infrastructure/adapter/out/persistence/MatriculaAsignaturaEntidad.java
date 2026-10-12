package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Lo que el estudiante matriculó en un período y con qué nota quedó (SCRUM-22). */
@Entity
@Table(name = "matricula_asignatura", schema = "cundiapp")
class MatriculaAsignaturaEntidad {

	@Id
	@Column(name = "id_matricula")
	private Integer id;

	@Column(name = "id_estudiante", nullable = false)
	private int idEstudiante;

	@Column(name = "codigo_asignatura", nullable = false)
	private String codigoAsignatura;

	@Column(name = "codigo_periodo", nullable = false)
	private String codigoPeriodo;

	@Column(name = "estado_matricula", nullable = false)
	private String estado;

	@Column(name = "nota_definitiva")
	private BigDecimal notaDefinitiva;

	protected MatriculaAsignaturaEntidad() {
	}

	String codigoAsignatura() {
		return codigoAsignatura;
	}

	String codigoPeriodo() {
		return codigoPeriodo;
	}

	String estado() {
		return estado;
	}

	BigDecimal notaDefinitiva() {
		return notaDefinitiva;
	}
}
