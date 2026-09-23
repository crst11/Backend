package co.edu.ucundinamarca.cundiapp.dominio.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import org.junit.jupiter.api.Test;

class CorreoInstitucionalTest {

	@Test
	void aceptaUnCorreoDelDominioInstitucional() {
		var correo = new CorreoInstitucional("juan.perez@ucundinamarca.edu.co");

		assertThat(correo.valor()).isEqualTo("juan.perez@ucundinamarca.edu.co");
	}

	@Test
	void quitaLosEspaciosSobrantes() {
		var correo = new CorreoInstitucional("  juan.perez@ucundinamarca.edu.co  ");

		assertThat(correo.valor()).isEqualTo("juan.perez@ucundinamarca.edu.co");
	}

	@Test
	void rechazaUnCorreoDeOtroDominio() {
		assertThatThrownBy(() -> new CorreoInstitucional("juan.perez@gmail.com"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("institucional");
	}

	@Test
	void rechazaUnCorreoVacioONulo() {
		assertThatThrownBy(() -> new CorreoInstitucional(""))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new CorreoInstitucional(null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}
}
