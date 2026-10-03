package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "prerrequisito", schema = "cundiapp")
class PrerrequisitoEntidad {

	@Embeddable
	static class Id implements Serializable {

		@Column(name = "codigo_asignatura")
		private String codigoAsignatura;

		@Column(name = "codigo_requerida")
		private String codigoRequerida;

		protected Id() {
		}

		@Override
		public boolean equals(Object otro) {
			if (this == otro) {
				return true;
			}
			if (!(otro instanceof Id that)) {
				return false;
			}
			return Objects.equals(codigoAsignatura, that.codigoAsignatura)
					&& Objects.equals(codigoRequerida, that.codigoRequerida);
		}

		@Override
		public int hashCode() {
			return Objects.hash(codigoAsignatura, codigoRequerida);
		}
	}

	@EmbeddedId
	private Id id;

	@Column(name = "tipo_requisito", nullable = false)
	private String tipo;

	protected PrerrequisitoEntidad() {
	}

	String codigoAsignatura() {
		return id.codigoAsignatura;
	}

	String codigoRequerida() {
		return id.codigoRequerida;
	}
}
