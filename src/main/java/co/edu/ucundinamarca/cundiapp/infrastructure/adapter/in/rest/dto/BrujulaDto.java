package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDelRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.ProyeccionDeNota;
import co.edu.ucundinamarca.cundiapp.domain.model.RiesgoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * La Brújula Académica: el riesgo de cada asignatura en curso, de lo más urgente a lo menos (RF09,
 * SCRUM-35). Nunca se llama «semáforo» en lo que ve el estudiante.
 *
 * <p>El nivel y el motivo los decide el dominio. La frase que los explica se arma aquí, porque es
 * presentación: el mismo motivo con otros números dice otra cosa.
 */
public record BrujulaDto(BigDecimal meta, ConfiguracionDto.UmbralesDto umbrales, List<ItemDto> asignaturas) {

	/**
	 * `nivel` es {@code alto}, {@code medio}, {@code bajo} o {@code sin_datos}. `notaRequerida` y
	 * `maximoPosible` vienen nulos cuando no hay datos, y `notaRequerida` también cuando la meta ya no
	 * se alcanza.
	 */
	public record ItemDto(
			AsignaturaMatriculadaDto asignatura,
			String nivel,
			String motivo,
			String explicacion,
			BigDecimal notaRequerida,
			BigDecimal maximoPosible) {
	}

	public static BrujulaDto desde(ConsultarMiBrujula.Resultado resultado) {
		UmbralesDeRiesgo umbrales = resultado.umbrales();
		List<ItemDto> items = resultado.asignaturas().stream()
				.map(item -> {
					RiesgoDeAsignatura riesgo = item.riesgo();
					ProyeccionDeNota proyeccion = riesgo.proyeccion();
					return new ItemDto(
							AsignaturaMatriculadaDto.desde(item.asignatura()),
							riesgo.nivel().name().toLowerCase(Locale.ROOT),
							riesgo.motivo().name().toLowerCase(Locale.ROOT),
							explicar(riesgo, umbrales),
							riesgo.notaRequerida(),
							proyeccion == null ? null : proyeccion.maximoPosible());
				})
				.toList();
		return new BrujulaDto(resultado.meta().valor(),
				new ConfiguracionDto.UmbralesDto(umbrales.medio(), umbrales.alto()), items);
	}

	/** «Necesitas 4.2 en lo que falta, más que tu umbral de riesgo medio (3.5)»: por qué está en ese nivel. */
	static String explicar(RiesgoDeAsignatura riesgo, UmbralesDeRiesgo umbrales) {
		MotivoDelRiesgo motivo = riesgo.motivo();
		if (motivo == MotivoDelRiesgo.SIN_ESTRUCTURA) {
			return "Cuéntanos cómo te evalúan en esta asignatura para poder calcular tu riesgo.";
		}
		ProyeccionDeNota proyeccion = riesgo.proyeccion();
		String meta = corto(proyeccion.meta().valor());
		return switch (motivo) {
			case INALCANZABLE -> "Aun sacando 5.0 en todo lo que falta llegarías a " + dos(proyeccion.maximoPosible())
					+ ", y tu meta es " + meta + ".";
			case META_ALCANZADA -> "Ya llegaste a tu meta de " + meta + ": llevas " + dos(proyeccion.puntos()) + " de 5.0.";
			case SOBRE_EL_UMBRAL_ALTO -> "Necesitas " + dos(proyeccion.notaRequerida())
					+ " en lo que falta, más que tu umbral de riesgo alto (" + corto(umbrales.alto()) + ").";
			case SOBRE_EL_UMBRAL_MEDIO -> "Necesitas " + dos(proyeccion.notaRequerida())
					+ " en lo que falta, más que tu umbral de riesgo medio (" + corto(umbrales.medio()) + ").";
			case DENTRO_DE_LO_ALCANZABLE -> "Necesitas " + dos(proyeccion.notaRequerida())
					+ " en lo que falta para llegar a " + meta + ", sin pasar de tu umbral de riesgo medio ("
					+ corto(umbrales.medio()) + ").";
			case SIN_ESTRUCTURA -> throw new IllegalStateException("ya se atendió arriba");
		};
	}

	private static String dos(BigDecimal valor) {
		return String.format(Locale.ROOT, "%.2f", valor);
	}

	/** 3.5 y no 3.50; con un tercer dígito significativo (3.25) se conserva. */
	private static String corto(BigDecimal valor) {
		BigDecimal limpio = valor.stripTrailingZeros();
		return limpio.scale() <= 1 ? String.format(Locale.ROOT, "%.1f", valor) : dos(valor);
	}
}
