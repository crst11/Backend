package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
class SesionId implements Serializable {

	@Column(name = "id_estudiante")
	private Integer idEstudiante;

	@Column(name = "consec_sesion")
	private Integer consecSesion;

	protected SesionId() {
	}

	SesionId(Integer idEstudiante, Integer consecSesion) {
		this.idEstudiante = idEstudiante;
		this.consecSesion = consecSesion;
	}

	Integer idEstudiante() {
		return idEstudiante;
	}

	Integer consecSesion() {
		return consecSesion;
	}

	@Override
	public boolean equals(Object otro) {
		if (this == otro) {
			return true;
		}
		if (!(otro instanceof SesionId that)) {
			return false;
		}
		return Objects.equals(idEstudiante, that.idEstudiante) && Objects.equals(consecSesion, that.consecSesion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idEstudiante, consecSesion);
	}
}
