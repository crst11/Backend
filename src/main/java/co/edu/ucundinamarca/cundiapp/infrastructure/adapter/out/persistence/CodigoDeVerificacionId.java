package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Desde SCRUM-68 un estudiante puede tener un código por propósito, no uno solo. */
@Embeddable
class CodigoDeVerificacionId implements Serializable {

	@Column(name = "id_estudiante")
	private Integer idEstudiante;

	@Column(name = "proposito")
	private String proposito;

	protected CodigoDeVerificacionId() {
	}

	CodigoDeVerificacionId(Integer idEstudiante, String proposito) {
		this.idEstudiante = idEstudiante;
		this.proposito = proposito;
	}

	Integer idEstudiante() {
		return idEstudiante;
	}

	String proposito() {
		return proposito;
	}

	@Override
	public boolean equals(Object otro) {
		if (this == otro) {
			return true;
		}
		if (!(otro instanceof CodigoDeVerificacionId that)) {
			return false;
		}
		return Objects.equals(idEstudiante, that.idEstudiante) && Objects.equals(proposito, that.proposito);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idEstudiante, proposito);
	}
}
