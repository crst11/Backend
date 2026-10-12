package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAsignatura;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Table(name = "asignatura", schema = "cundiapp")
class AsignaturaEntidad {

	@Id
	@Column(name = "codigo_asignatura")
	private String codigo;

	@Column(name = "codigo_plan", nullable = false)
	private String codigoPlan;

	@Column(name = "nombre_asignatura", nullable = false)
	private String nombre;

	@Column(name = "creditos", nullable = false)
	private int creditos;

	@Column(name = "tipo_asignatura", nullable = false)
	private String tipo;

	@Column(name = "periodo_sugerido")
	private Integer periodoSugerido;

	protected AsignaturaEntidad() {
	}

	String codigo() {
		return codigo;
	}

	String nombre() {
		return nombre;
	}

	int creditos() {
		return creditos;
	}

	Asignatura aDominio(List<String> prerrequisitos) {
		return new Asignatura(
				codigo, nombre, creditos, TipoDeAsignatura.desdeBd(tipo), periodoSugerido, prerrequisitos);
	}
}
