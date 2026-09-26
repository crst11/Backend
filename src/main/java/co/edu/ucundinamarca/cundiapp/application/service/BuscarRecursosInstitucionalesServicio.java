package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.BuscarRecursosInstitucionales;
import co.edu.ucundinamarca.cundiapp.application.port.out.RecursoInstitucionalRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.RecursoInstitucional;
import java.util.List;

/**
 * La guía tiene decenas de documentos, no miles: se leen los publicados y la regla de búsqueda del
 * dominio decide cuáles coinciden. Así la búsqueda se prueba sin base de datos.
 */
public class BuscarRecursosInstitucionalesServicio implements BuscarRecursosInstitucionales {

	static final int LARGO_MAXIMO_BUSQUEDA = 80;

	private final RecursoInstitucionalRepositorio repositorio;

	public BuscarRecursosInstitucionalesServicio(RecursoInstitucionalRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public List<RecursoInstitucional> ejecutar(String busqueda, Integer idCategoria) {
		if (busqueda != null && busqueda.length() > LARGO_MAXIMO_BUSQUEDA) {
			throw new ReglaDeNegocioVioladaException(
					"La búsqueda puede tener máximo " + LARGO_MAXIMO_BUSQUEDA + " caracteres");
		}
		return repositorio.listarPublicados().stream()
				.filter(recurso -> idCategoria == null || idCategoria.equals(recurso.categoria().id()))
				.filter(recurso -> recurso.coincideCon(busqueda))
				.toList();
	}
}
