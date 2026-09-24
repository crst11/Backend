package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;

/** Entrega el código al correo del estudiante; el canal real (SMTP u otro) es un adaptador. */
public interface EnviadorDeCodigoPort {

	void enviar(CorreoInstitucional destino, String codigo);
}
