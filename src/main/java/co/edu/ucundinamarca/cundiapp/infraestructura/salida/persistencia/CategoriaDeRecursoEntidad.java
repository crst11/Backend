package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CategoriaDeRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria_de_recurso", schema = "cundiapp")
class CategoriaDeRecursoEntidad {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_categoria")
	private Integer id;

	@Column(name = "nombre_categoria")
	private String nombre;

	@Column(name = "orden")
	private Integer orden;

	protected CategoriaDeRecursoEntidad() {
	}

	CategoriaDeRecurso aDominio() {
		return new CategoriaDeRecurso(id, nombre);
	}
}
