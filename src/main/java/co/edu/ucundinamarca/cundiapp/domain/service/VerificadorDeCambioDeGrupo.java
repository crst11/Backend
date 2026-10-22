package co.edu.ucundinamarca.cundiapp.domain.service;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaOfertada;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeOferta;
import co.edu.ucundinamarca.cundiapp.domain.model.ChoqueDelGrupo;
import co.edu.ucundinamarca.cundiapp.domain.model.EleccionDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.GrupoOfertado;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDelCandidato;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dice, para cada grupo candidato de una materia, si cabe en el horario armado o con qué choca (RF13,
 * SCRUM-245). Sirve para cambiar de grupo sin romper el horario: por ejemplo, ver si el 501 sirve en lugar del
 * 502 que sugirió la app, o elegir el grupo de un docente en particular cuando dos grupos tienen la misma hora.
 *
 * <p>La materia que se cambia no cuenta contra sus propios candidatos. Terminar a la hora en que empieza otra
 * clase no es choque (regla de {@link BloqueDeOferta#chocaCon}). No guarda nada ni usa la base de datos.
 */
public class VerificadorDeCambioDeGrupo {

	/** Las materias que puede tener un horario armado: las mismas que acepta el armador. */
	public static final int MAXIMO_DE_ELECCIONES = ArmadorDeHorario.MAXIMO_DE_ASIGNATURAS;
	/** Los grupos que puede tener una materia: los mismos que acepta la oferta. */
	public static final int MAXIMO_DE_CANDIDATOS = AsignaturaOfertada.MAXIMO_DE_GRUPOS;

	public List<ResultadoDelCandidato> verificar(
			List<EleccionDeHorario> horario, String asignaturaACambiar, List<GrupoOfertado> candidatos) {
		if (asignaturaACambiar == null || asignaturaACambiar.isBlank()) {
			throw new ReglaDeNegocioVioladaException("Falta la materia que quieres cambiar");
		}
		if (candidatos == null || candidatos.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("Falta al menos un grupo para probar");
		}
		if (horario.size() > MAXIMO_DE_ELECCIONES) {
			throw new ReglaDeNegocioVioladaException(
					"El horario no puede tener más de %d materias".formatted(MAXIMO_DE_ELECCIONES));
		}
		if (candidatos.size() > MAXIMO_DE_CANDIDATOS) {
			throw new ReglaDeNegocioVioladaException(
					"No se pueden probar más de %d grupos a la vez".formatted(MAXIMO_DE_CANDIDATOS));
		}
		String cambiada = comparable(asignaturaACambiar);
		List<EleccionDeHorario> restantes =
				horario.stream().filter(eleccion -> !comparable(eleccion.asignatura()).equals(cambiada)).toList();
		return candidatos.stream().map(candidato -> resultadoDe(candidato, restantes)).toList();
	}

	private static ResultadoDelCandidato resultadoDe(GrupoOfertado candidato, List<EleccionDeHorario> restantes) {
		List<ChoqueDelGrupo> choques = new ArrayList<>();
		for (EleccionDeHorario eleccion : restantes) {
			for (BloqueDeOferta ajeno : eleccion.grupo().bloques()) {
				boolean choca = candidato.bloques().stream().anyMatch(propio -> propio.chocaCon(ajeno));
				if (choca) {
					choques.add(new ChoqueDelGrupo(
							eleccion.asignatura(), eleccion.grupo().codigo(), ajeno.dia(), ajeno.inicio(), ajeno.fin()));
				}
			}
		}
		return new ResultadoDelCandidato(candidato.codigo(), choques.isEmpty(), choques);
	}

	private static String comparable(String nombre) {
		return nombre.strip().toLowerCase(Locale.ROOT);
	}
}
