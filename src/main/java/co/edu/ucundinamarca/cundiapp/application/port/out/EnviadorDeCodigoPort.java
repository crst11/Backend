package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;

/** Entrega el código al correo del estudiante; el canal real (SMTP u otro) es un adaptador. */
public interface EnviadorDeCodigoPort {

	/** El propósito viaja con el código para que el mensaje diga para qué sirve y qué hacer si no se pidió. */
	void enviar(CorreoInstitucional destino, String codigo, PropositoDelCodigo proposito);
}
