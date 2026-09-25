package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeRecurso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Prueba de integración contra PostgreSQL real (Testcontainers), con las migraciones de Flyway
 * ya aplicadas: verifica que el adaptador lee las categorías de arranque en el orden correcto.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CategoriaDeRecursoAdaptadorTest {

	@Autowired
	private CategoriaDeRecursoRepositorio repositorio;

	@Test
	void buscaTodasLasCategoriasDeArranqueEnOrden() {
		var categorias = repositorio.buscarTodas();

		assertThat(categorias)
				.extracting(CategoriaDeRecurso::nombre)
				.containsExactly("Reglamentos", "Formatos", "Convocatorias");
		assertThat(categorias).allSatisfy(c -> assertThat(c.id()).isNotNull());
	}
}
