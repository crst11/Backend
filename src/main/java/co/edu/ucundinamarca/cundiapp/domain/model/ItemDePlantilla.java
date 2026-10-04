package co.edu.ucundinamarca.cundiapp.domain.model;

import java.math.BigDecimal;

/** Un nivel de una plantilla de evaluación: "Primer corte, 30 %" (RF05). */
public record ItemDePlantilla(int consecutivo, String nombre, BigDecimal porcentaje) {

	public ItemDePlantilla {
		ReglasDeLaEstructura.consecutivoValido(consecutivo, "el ítem de la plantilla");
		ReglasDeLaEstructura.nombreValido(nombre, CategoriaDeEvaluacion.LARGO_DEL_NOMBRE, "el ítem de la plantilla");
		ReglasDeLaEstructura.porcentajeValido(porcentaje, "el ítem «%s»".formatted(nombre));
	}
}
