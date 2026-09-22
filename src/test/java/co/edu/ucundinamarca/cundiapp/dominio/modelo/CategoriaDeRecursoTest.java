package co.edu.ucundinamarca.cundiapp.dominio.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import org.junit.jupiter.api.Test;

/** Dominio puro: sin Spring, sin base de datos. */
class CategoriaDeRecursoTest {

	@Test
	void seCreaConUnNombreValido() {
		var categoria = new CategoriaDeRecurso(1, "Reglamentos");

		assertThat(categoria.id()).isEqualTo(1);
		assertThat(categoria.nombre()).isEqualTo("Reglamentos");
	}

	@Test
	void rechazaUnNombreVacioOEnBlanco() {
		assertThatThrownBy(() -> new CategoriaDeRecurso(1, ""))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("nombre");
		assertThatThrownBy(() -> new CategoriaDeRecurso(1, "   "))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void rechazaUnNombreNulo() {
		assertThatThrownBy(() -> new CategoriaDeRecurso(1, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}
}
