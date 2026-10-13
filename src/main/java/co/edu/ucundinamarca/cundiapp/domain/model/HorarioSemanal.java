package co.edu.ucundinamarca.cundiapp.domain.model;

import java.text.Collator;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * El horario de la semana del estudiante: todas sus clases (RF04, SCRUM-25 y SCRUM-26).
 *
 * <p>Cómo se lee un horario, por día y en orden, y cómo se filtra, son reglas de dominio y no un
 * asunto de la pantalla: la vista por día y la vista por semana piden lo mismo y no vuelven a
 * ordenar ni a filtrar nada. Dos clases a la misma hora se muestran las dos; avisar de un choque es
 * otra historia.
 */
public record HorarioSemanal(List<ClaseDelHorario> clases) {

	/** El orden alfabético del español: «Álgebra» va con la A y no después de la Z. */
	private static final Collator ESPANOL = Collator.getInstance(Locale.forLanguageTag("es"));

	private static final Comparator<ClaseDelHorario> POR_HORA = Comparator
			.<ClaseDelHorario, LocalTime>comparing(clase -> clase.bloque().horaInicio())
			.thenComparing(clase -> clase.bloque().horaFin())
			.thenComparing(ClaseDelHorario::asignatura, ESPANOL);

	private static final Comparator<ClaseDelHorario> POR_ASIGNATURA = Comparator
			.comparing(ClaseDelHorario::asignatura, ESPANOL)
			.thenComparing(clase -> clase.bloque().horaInicio())
			.thenComparing(clase -> clase.bloque().horaFin());

	public HorarioSemanal {
		clases = List.copyOf(clases);
	}

	public boolean estaVacio() {
		return clases.isEmpty();
	}

	public int totalDeClases() {
		return clases.size();
	}

	/** Las clases que pasan el filtro. Un filtro que no coincide con nada devuelve un horario vacío. */
	public HorarioSemanal filtrar(FiltroDeHorario filtro) {
		return new HorarioSemanal(clases.stream().filter(filtro::admite).toList());
	}

	/** Las clases de un día, en orden de hora. Vacía, y no un error, si ese día no hay clases. */
	public List<ClaseDelHorario> delDia(DiaDeLaSemana dia) {
		return delDia(dia, OrdenDelHorario.HORA);
	}

	public List<ClaseDelHorario> delDia(DiaDeLaSemana dia, OrdenDelHorario orden) {
		Comparator<ClaseDelHorario> comparador = orden == OrdenDelHorario.ASIGNATURA ? POR_ASIGNATURA : POR_HORA;
		return clases.stream().filter(clase -> clase.bloque().dia() == dia).sorted(comparador).toList();
	}

	/** Solo los días que tienen clases, de lunes a domingo. */
	public List<DiaConClases> dias() {
		return dias(OrdenDelHorario.HORA);
	}

	public List<DiaConClases> dias(OrdenDelHorario orden) {
		return Arrays.stream(DiaDeLaSemana.values())
				.map(dia -> new DiaConClases(dia, delDia(dia, orden)))
				.filter(dia -> !dia.clases().isEmpty())
				.toList();
	}

	/**
	 * Las sedes en las que hay clases, sin repetir y en orden alfabético: lo que se puede elegir en el
	 * filtro. «Fusagasugá» y «fusagasuga» cuentan como una sola, con la escritura de la primera.
	 */
	public List<String> sedes() {
		Map<String, String> porNombreNormalizado = new LinkedHashMap<>();
		for (ClaseDelHorario clase : clases) {
			clase.bloque().espacio().map(EspacioFisico::sede)
					.ifPresent(sede -> porNombreNormalizado.putIfAbsent(TextoNormalizado.de(sede), sede));
		}
		return porNombreNormalizado.values().stream().sorted(ESPANOL).toList();
	}

	/** Las asignaturas que tienen clases, sin repetir y por nombre: lo que se puede elegir en el filtro. */
	public List<AsignaturaDelHorario> asignaturas() {
		Map<String, AsignaturaDelHorario> porCodigo = new LinkedHashMap<>();
		for (ClaseDelHorario clase : clases) {
			porCodigo.putIfAbsent(clase.codigoAsignatura(),
					new AsignaturaDelHorario(clase.codigoAsignatura(), clase.asignatura()));
		}
		return porCodigo.values().stream().sorted(Comparator.comparing(AsignaturaDelHorario::nombre, ESPANOL)).toList();
	}
}
