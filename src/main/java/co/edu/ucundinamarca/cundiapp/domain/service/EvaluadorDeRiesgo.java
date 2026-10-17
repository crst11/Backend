package co.edu.ucundinamarca.cundiapp.domain.service;

import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDelRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.ProyeccionDeNota;
import co.edu.ucundinamarca.cundiapp.domain.model.RiesgoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;

/**
 * El riesgo de una asignatura a partir de la nota que hace falta para llegar a la meta (RF09,
 * SCRUM-35).
 *
 * <p>Alto si la meta ya no se alcanza o si la nota que hace falta pasa del umbral alto; medio si pasa
 * del medio; bajo en el resto, que incluye la meta que ya se alcanzó. «Pasa» es estrictamente mayor:
 * exactamente 3.5 sigue siendo bajo y exactamente 4.5 sigue siendo medio, como dice el criterio de
 * aceptación.
 */
public class EvaluadorDeRiesgo {

	public RiesgoDeAsignatura evaluar(ProyeccionDeNota proyeccion, UmbralesDeRiesgo umbrales) {
		return switch (proyeccion.situacion()) {
			case INALCANZABLE -> new RiesgoDeAsignatura(NivelDeRiesgo.ALTO, MotivoDelRiesgo.INALCANZABLE, proyeccion);
			case ALCANZADA -> new RiesgoDeAsignatura(NivelDeRiesgo.BAJO, MotivoDelRiesgo.META_ALCANZADA, proyeccion);
			case ALCANZABLE -> segunLaNotaRequerida(proyeccion, umbrales);
		};
	}

	private static RiesgoDeAsignatura segunLaNotaRequerida(ProyeccionDeNota proyeccion, UmbralesDeRiesgo umbrales) {
		if (proyeccion.notaRequerida().compareTo(umbrales.alto()) > 0) {
			return new RiesgoDeAsignatura(NivelDeRiesgo.ALTO, MotivoDelRiesgo.SOBRE_EL_UMBRAL_ALTO, proyeccion);
		}
		if (proyeccion.notaRequerida().compareTo(umbrales.medio()) > 0) {
			return new RiesgoDeAsignatura(NivelDeRiesgo.MEDIO, MotivoDelRiesgo.SOBRE_EL_UMBRAL_MEDIO, proyeccion);
		}
		return new RiesgoDeAsignatura(NivelDeRiesgo.BAJO, MotivoDelRiesgo.DENTRO_DE_LO_ALCANZABLE, proyeccion);
	}
}
