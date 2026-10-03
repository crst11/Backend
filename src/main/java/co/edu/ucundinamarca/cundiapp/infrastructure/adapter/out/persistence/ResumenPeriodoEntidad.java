package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.ResumenOficialDePeriodo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/** Los totales que la universidad publica de cada período, tal como llegaron en el reporte. */
@Entity
@Table(name = "resumen_periodo", schema = "cundiapp")
class ResumenPeriodoEntidad {

	@EmbeddedId
	private Id id;

	@Column(name = "creditos_matriculados", nullable = false)
	private int creditosMatriculados;

	@Column(name = "creditos_aprobados", nullable = false)
	private int creditosAprobados;

	@Column(name = "promedio_periodo")
	private BigDecimal promedioPeriodo;

	@Column(name = "promedio_acumulado")
	private BigDecimal promedioAcumulado;

	protected ResumenPeriodoEntidad() {
	}

	ResumenOficialDePeriodo aDominio() {
		return new ResumenOficialDePeriodo(
				id.codigoPeriodo, creditosMatriculados, creditosAprobados, promedioPeriodo, promedioAcumulado);
	}

	@Embeddable
	static class Id implements Serializable {

		@Column(name = "id_estudiante")
		private int idEstudiante;

		@Column(name = "codigo_periodo")
		private String codigoPeriodo;

		protected Id() {
		}

		@Override
		public boolean equals(Object otro) {
			return otro instanceof Id id
					&& idEstudiante == id.idEstudiante
					&& Objects.equals(codigoPeriodo, id.codigoPeriodo);
		}

		@Override
		public int hashCode() {
			return Objects.hash(idEstudiante, codigoPeriodo);
		}
	}
}
