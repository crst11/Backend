package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Los umbrales de riesgo (RF09, SCRUM-36): las mismas reglas que la base de datos, con mensajes útiles. */
class UmbralesDeRiesgoTest {

	private static UmbralesDeRiesgo umbrales(String medio, String alto) {
		return new UmbralesDeRiesgo(new BigDecimal(medio), new BigDecimal(alto));
	}

	@Test
	void porDefectoSon3Punto5Y4Punto5() {
		assertThat(UmbralesDeRiesgo.PREDETERMINADOS.medio()).isEqualByComparingTo("3.5");
		assertThat(UmbralesDeRiesgo.PREDETERMINADOS.alto()).isEqualByComparingTo("4.5");
	}

	@Test
	void aceptaLosExtremosDeLaEscalaMientrasElMedioSeaMenor() {
		assertThat(umbrales("0", "5").alto()).isEqualByComparingTo("5");
		assertThat(umbrales("3.25", "3.26").medio()).isEqualByComparingTo("3.25");
	}

	@Test
	void faltandoUnoSeRechaza() {
		assertThatThrownBy(() -> new UmbralesDeRiesgo(null, BigDecimal.ONE))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("Faltan");
		assertThatThrownBy(() -> new UmbralesDeRiesgo(BigDecimal.ONE, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("Faltan");
	}

	@Test
	void fueraDeLaEscalaSeRechaza() {
		assertThatThrownBy(() -> umbrales("-0.1", "4"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("0.0 y 5.0");
		assertThatThrownBy(() -> umbrales("3", "5.1"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("0.0 y 5.0");
	}

	@Test
	void conMasDeDosDecimalesSeRechaza() {
		assertThatThrownBy(() -> umbrales("3.505", "4.5"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("dos decimales");
	}

	@Test
	void elMedioTieneQueSerMenorQueElAlto() {
		assertThatThrownBy(() -> umbrales("4.5", "4.5"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("menor que el alto");
		assertThatThrownBy(() -> umbrales("4.6", "4.5"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("menor que el alto");
	}
}
