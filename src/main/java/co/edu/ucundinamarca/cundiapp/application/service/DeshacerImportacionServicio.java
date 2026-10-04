package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.DeshacerImportacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Importacion;

/**
 * Deshace una carga (RF03, SCRUM-24).
 *
 * <p>Deshacer no es borrar: lo que la carga creó se elimina, pero lo que pisó vuelve al valor que
 * tenía. Por eso cada importación guarda un respaldo de lo que toca.
 *
 * <p>Solo se deshace la última carga confirmada. Deshacer una del medio dejaría encima los cambios
 * de las que vinieron después, y el resultado no sería el estado anterior de nada.
 */
public class DeshacerImportacionServicio implements DeshacerImportacion {

	private final ImportacionRepositorio importaciones;

	public DeshacerImportacionServicio(ImportacionRepositorio importaciones) {
		this.importaciones = importaciones;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, int idImportacion) {
		Importacion importacion = importaciones.buscar(idEstudiante, idImportacion)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"No encontramos esa carga en tu historial"));

		if (!importacion.sePuedeDeshacer()) {
			throw new ReglaDeNegocioVioladaException("Esa carga ya se deshizo o nunca llegó a guardarse");
		}
		if (!esLaUltima(idEstudiante, idImportacion)) {
			throw new ReglaDeNegocioVioladaException(
					"Solo se puede deshacer la última carga: deshacer una anterior dejaría encima los cambios de las que vinieron después");
		}

		return importaciones.revertir(idEstudiante, idImportacion);
	}

	private boolean esLaUltima(int idEstudiante, int idImportacion) {
		return importaciones.deEstudiante(idEstudiante).stream()
				.filter(Importacion::sePuedeDeshacer)
				.findFirst()
				.filter(ultima -> ultima.id() == idImportacion)
				.isPresent();
	}
}
