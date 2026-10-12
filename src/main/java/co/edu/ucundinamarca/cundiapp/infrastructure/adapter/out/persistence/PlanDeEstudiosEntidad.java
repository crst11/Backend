package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan_de_estudios", schema = "cundiapp")
class PlanDeEstudiosEntidad {

	@Id
	@Column(name = "codigo_plan")
	private String codigo;

	@Column(name = "codigo_programa", nullable = false)
	private String codigoPrograma;

	@Column(name = "version", nullable = false)
	private String version;

	@Column(name = "anio_vigencia", nullable = false)
	private int anioVigencia;

	@Column(name = "estado_plan", nullable = false)
	private String estado;

	protected PlanDeEstudiosEntidad() {
	}

	String codigo() {
		return codigo;
	}

	String codigoPrograma() {
		return codigoPrograma;
	}
}
