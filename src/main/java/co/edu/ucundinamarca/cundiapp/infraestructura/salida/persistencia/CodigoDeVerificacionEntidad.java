package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CodigoDeVerificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "codigo_verificacion", schema = "cundiapp")
class CodigoDeVerificacionEntidad {

	@Id
	@Column(name = "id_estudiante")
	private Integer idEstudiante;

	@Column(name = "hash_codigo", nullable = false)
	private String huella;

	@Column(name = "fecha_emision", nullable = false)
	private Instant fechaEmision;

	@Column(name = "fecha_expiracion", nullable = false)
	private Instant fechaExpiracion;

	@Column(name = "intentos_fallidos", nullable = false)
	private int intentosFallidos;

	@Column(name = "fecha_uso")
	private Instant fechaUso;

	protected CodigoDeVerificacionEntidad() {
	}

	static CodigoDeVerificacionEntidad desde(CodigoDeVerificacion codigo) {
		CodigoDeVerificacionEntidad entidad = new CodigoDeVerificacionEntidad();
		entidad.idEstudiante = codigo.idEstudiante();
		entidad.huella = codigo.huella();
		entidad.fechaEmision = codigo.fechaEmision();
		entidad.fechaExpiracion = codigo.fechaExpiracion();
		entidad.intentosFallidos = codigo.intentosFallidos();
		entidad.fechaUso = codigo.fechaUso();
		return entidad;
	}

	CodigoDeVerificacion aDominio() {
		return new CodigoDeVerificacion(idEstudiante, huella, fechaEmision, fechaExpiracion, intentosFallidos, fechaUso);
	}
}
