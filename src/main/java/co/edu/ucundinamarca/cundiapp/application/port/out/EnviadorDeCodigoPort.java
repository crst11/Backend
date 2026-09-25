package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;

/** Entrega el código al correo del estudiante; el canal real (SMTP u otro) es un adaptador. */
public interface EnviadorDeCodigoPort {

	void enviar(CorreoInstitucional destino, String codigo);
}
