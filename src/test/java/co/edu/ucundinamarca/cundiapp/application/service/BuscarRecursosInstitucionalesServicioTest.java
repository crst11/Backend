package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import co.edu.ucundinamarca.cundiapp.application.port.out.RecursoInstitucionalRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeRecurso;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeRecurso;
import co.edu.ucundinamarca.cundiapp.domain.model.RecursoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeRecurso;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuscarRecursosInstitucionalesServicioTest {

	private static final CategoriaDeRecurso REGLAMENTOS = new CategoriaDeRecurso(1, "Reglamentos");
	private static final CategoriaDeRecurso TRAMITES = new CategoriaDeRecurso(2, "Trámites y calendario");

	private final RecursoInstitucionalRepositorio repositorio = mock(RecursoInstitucionalRepositorio.class);
	private final BuscarRecursosInstitucionalesServicio servicio = new BuscarRecursosInstitucionalesServicio(repositorio);

	private static RecursoInstitucional recurso(int id, CategoriaDeRecurso categoria, String titulo) {
		return new RecursoInstitucional(id, categoria, titulo, null, "https://www.ucundinamarca.edu.co/" + id,
				TipoDeRecurso.DOCUMENTO, EstadoDeRecurso.VIGENTE, LocalDate.of(2026, 9, 26));
	}

	private final RecursoInstitucional reglamento = recurso(1, REGLAMENTOS, "Reglamento Estudiantil");
	private final RecursoInstitucional calendario = recurso(2, TRAMITES, "Calendario académico 2026-2");

	@BeforeEach
	void configurar() {
		given(repositorio.listarPublicados()).willReturn(List.of(reglamento, calendario));
	}

	@Test
	void sinFiltrosDevuelveTodaLaGuiaEnElOrdenDelRepositorio() {
		assertThat(servicio.ejecutar(null, null)).containsExactly(reglamento, calendario);
	}

	@Test
	void buscaPorTexto() {
		assertThat(servicio.ejecutar("calendario academico", null)).containsExactly(calendario);
		assertThat(servicio.ejecutar("no existe", null)).isEmpty();
	}

	@Test
	void filtraPorCategoriaYCombinaConLaBusqueda() {
		assertThat(servicio.ejecutar(null, 1)).containsExactly(reglamento);
		assertThat(servicio.ejecutar("calendario", 1)).isEmpty();
	}

	@Test
	void rechazaUnaBusquedaDemasiadoLarga() {
		assertThatThrownBy(() -> servicio.ejecutar("a".repeat(81), null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("80");
	}
}
