package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMiMeta;
import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMisUmbrales;
import co.edu.ucundinamarca.cundiapp.application.port.in.DefinirMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RevisarCambiosDeRiesgo;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Envuelve los casos de uso que pueden mover el riesgo de una asignatura para que, después de cada uno,
 * se revise si el nivel cambió y se avise (RF10, SCRUM-50): anotar o quitar una nota, definir la estructura
 * de evaluación, cambiar la meta y cambiar los umbrales.
 *
 * <p>La revisión va después de que la acción ya quedó guardada y **nunca la tumba**: si revisar falla (la base
 * de datos, un aviso mal formado), se anota en el log y la respuesta de la persona sale igual. Perder un aviso
 * es mejor que decirle que no se guardó una nota que sí se guardó. Vive aquí, y no dentro de los casos de uso,
 * para que estos sigan sabiendo una sola cosa y para que el aislamiento de fallos esté en un único lugar.
 */
class RiesgoVigilado {

	private static final Logger log = LoggerFactory.getLogger(RiesgoVigilado.class);

	private final RevisarCambiosDeRiesgo revisar;

	RiesgoVigilado(RevisarCambiosDeRiesgo revisar) {
		this.revisar = revisar;
	}

	RegistrarMiCalificacion vigilar(RegistrarMiCalificacion caso) {
		return (idEstudiante, idMatricula, consecCategoria, consecActividad, valor) -> {
			var resultado = caso.ejecutar(idEstudiante, idMatricula, consecCategoria, consecActividad, valor);
			revisarSinTumbar(idEstudiante, r -> r.deLaAsignatura(idEstudiante, idMatricula));
			return resultado;
		};
	}

	QuitarMiCalificacion vigilar(QuitarMiCalificacion caso) {
		return (idEstudiante, idMatricula, consecCategoria, consecActividad) -> {
			var resultado = caso.ejecutar(idEstudiante, idMatricula, consecCategoria, consecActividad);
			revisarSinTumbar(idEstudiante, r -> r.deLaAsignatura(idEstudiante, idMatricula));
			return resultado;
		};
	}

	DefinirMiEstructuraDeEvaluacion vigilar(DefinirMiEstructuraDeEvaluacion caso) {
		return (idEstudiante, idMatricula, categorias) -> {
			var resultado = caso.ejecutar(idEstudiante, idMatricula, categorias);
			revisarSinTumbar(idEstudiante, r -> r.deLaAsignatura(idEstudiante, idMatricula));
			return resultado;
		};
	}

	CambiarMiMeta vigilar(CambiarMiMeta caso) {
		return (idEstudiante, meta) -> {
			var resultado = caso.ejecutar(idEstudiante, meta);
			revisarSinTumbar(idEstudiante, r -> r.deTodasLasAsignaturas(idEstudiante));
			return resultado;
		};
	}

	CambiarMisUmbrales vigilar(CambiarMisUmbrales caso) {
		return (idEstudiante, umbrales) -> {
			var resultado = caso.ejecutar(idEstudiante, umbrales);
			revisarSinTumbar(idEstudiante, r -> r.deTodasLasAsignaturas(idEstudiante));
			return resultado;
		};
	}

	private void revisarSinTumbar(int idEstudiante, Consumer<RevisarCambiosDeRiesgo> revision) {
		try {
			revision.accept(revisar);
		} catch (RuntimeException e) {
			log.warn("No pudimos revisar el cambio de riesgo del estudiante {}", idEstudiante, e);
		}
	}
}
