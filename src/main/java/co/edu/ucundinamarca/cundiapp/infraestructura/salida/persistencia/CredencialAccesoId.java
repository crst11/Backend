package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
class CredencialAccesoId implements Serializable {

	@Column(name = "id_estudiante")
	private Integer idEstudiante;

	@Column(name = "proveedor")
	private String proveedor;

	protected CredencialAccesoId() {
	}

	CredencialAccesoId(Integer idEstudiante, String proveedor) {
		this.idEstudiante = idEstudiante;
		this.proveedor = proveedor;
	}

	@Override
	public boolean equals(Object otro) {
		if (this == otro) {
			return true;
		}
		if (!(otro instanceof CredencialAccesoId that)) {
			return false;
		}
		return Objects.equals(idEstudiante, that.idEstudiante) && Objects.equals(proveedor, that.proveedor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idEstudiante, proveedor);
	}
}
