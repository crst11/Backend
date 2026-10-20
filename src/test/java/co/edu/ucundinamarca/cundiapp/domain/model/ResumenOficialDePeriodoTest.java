package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Las invariantes de los créditos de un período (SCRUM-79).
 *
 * <p>Nadie aprueba más créditos de los que matriculó. La regla está aquí y no en el DTO porque el
 * estudiante puede corregir esos dos números antes de confirmar la importación: no es una regla de
 * formato, es una del negocio, y el mensaje tiene que decirle cuáles son las cifras que no cuadran.
 */
class ResumenOficialDePeriodoTest {

	private static ResumenOficialDePeriodo resumen(int matriculados, int aprobados) {
		ResumenOficialDePeriodo.verificarCreditos("2024-2", matriculados, aprobados);
		return new ResumenOficialDePeriodo(
				"2024-2", matriculados, aprobados, new BigDecimal("4.2"), new BigDecimal("4.2"));
	}

	@Test
	void aceptaUnPeriodoEnQueSeAproboTodoLoMatriculado() {
		assertThatCode(() -> resumen(16, 16)).doesNotThrowAnyException();
	}

	@Test
	void aceptaUnPeriodoConAsignaturasPerdidas() {
		assertThat(resumen(16, 13).creditosAprobados()).isEqualTo(13);
	}

	/** Un período en curso todavía no tiene créditos aprobados, y eso es válido. */
	@Test
	void aceptaCeroAprobados() {
		assertThatCode(() -> resumen(16, 0)).doesNotThrowAnyException();
	}

	/** La ruta de aprendizaje tiene asignaturas de 0 créditos (diagnóstico y nivelatorio). */
	@Test
	void aceptaUnPeriodoSinCreditos() {
		assertThatCode(() -> resumen(0, 0)).doesNotThrowAnyException();
	}

	@Test
	void rechazaMasAprobadosQueMatriculadosYNombraLasDosCifras() {
		assertThatThrownBy(() -> resumen(3, 20))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("2024-2")
				.hasMessageContaining("20 créditos aprobados")
				.hasMessageContaining("3 matriculados");
	}

	@Test
	void rechazaUnExcesoDeUnSoloCredito() {
		assertThatThrownBy(() -> resumen(16, 17))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void rechazaCreditosNegativos() {
		assertThatThrownBy(() -> resumen(-1, 0))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("no pueden ser negativos");
		assertThatThrownBy(() -> resumen(16, -1))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("no pueden ser negativos");
	}

	/** Los promedios siguen siendo opcionales: un período sin nota publicada no es un error. */
	@Test
	void losPromediosPuedenVenirVacios() {
		var sinPromedios = new ResumenOficialDePeriodo("2024-2", 16, 16, null, null);

		assertThat(sinPromedios.promedioDelPeriodo()).isEmpty();
		assertThat(sinPromedios.acumulado()).isEmpty();
	}

	/** El lector del PDF arma el registro con lo que lee: un PDF mal leído no debe tumbar el análisis (el estudiante lo corrige). */
	@Test
	void construirElRegistroNoValidaLosCreditos() {
		assertThatCode(() -> new ResumenOficialDePeriodo("2024-2", 3, 20, null, null)).doesNotThrowAnyException();
	}
}
