package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "programa_academico", schema = "cundiapp")
class ProgramaAcademicoEntidad {

	@Id
	@Column(name = "codigo_programa")
	private String codigo;

	@Column(name = "nombre_programa", nullable = false)
	private String nombre;

	@Column(name = "facultad", nullable = false)
	private String facultad;

	@Column(name = "sede", nullable = false)
	private String sede;

	@Column(name = "total_creditos", nullable = false)
	private int totalCreditos;

	@Column(name = "numero_periodos", nullable = false)
	private int numeroPeriodos;

	protected ProgramaAcademicoEntidad() {
	}

	ProgramaAcademico aDominio() {
		return new ProgramaAcademico(codigo, nombre, facultad, sede, totalCreditos, numeroPeriodos);
	}
}
