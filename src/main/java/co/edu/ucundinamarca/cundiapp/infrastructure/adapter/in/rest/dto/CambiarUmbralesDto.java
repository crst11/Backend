package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Los umbrales nuevos (SCRUM-36). Solo se exige que vengan los dos: que estén entre 0.0 y 5.0, que no
 * pasen de dos decimales y que el medio sea menor que el alto lo comprueba el dominio, para que el
 * mensaje sea el mismo venga de donde venga.
 */
public record CambiarUmbralesDto(@NotNull BigDecimal medio, @NotNull BigDecimal alto) {

	public UmbralesDeRiesgo aUmbrales() {
		return new UmbralesDeRiesgo(medio, alto);
	}
}
