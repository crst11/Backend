package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeRecurso;
import co.edu.ucundinamarca.cundiapp.domain.model.RecursoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Solo lectura: los recursos se cargan con migraciones (V4) y la API nunca los modifica. */
@Entity
@Table(name = "recurso_institucional", schema = "cundiapp")
class RecursoInstitucionalEntidad {

	@Id
	@Column(name = "id_recurso")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_categoria")
	private CategoriaDeRecursoEntidad categoria;

	@Column(name = "titulo", nullable = false)
	private String titulo;

	@Column(name = "descripcion")
	private String descripcion;

	@Column(name = "url", nullable = false)
	private String url;

	@Column(name = "tipo_recurso", nullable = false)
	private String tipo;

	@Column(name = "requiere_autenticacion", nullable = false)
	private boolean requiereAutenticacion;

	@Column(name = "fecha_verificacion")
	private LocalDate fechaVerificacion;

	@Column(name = "estado_recurso", nullable = false)
	private String estado;

	protected RecursoInstitucionalEntidad() {
	}

	RecursoInstitucional aDominio() {
		return new RecursoInstitucional(
				id,
				categoria.aDominio(),
				titulo,
				descripcion,
				url,
				TipoDeRecurso.desdeBd(tipo),
				EstadoDeRecurso.desdeBd(estado),
				fechaVerificacion);
	}
}
