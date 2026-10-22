package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaOfertada;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeOferta;
import co.edu.ucundinamarca.cundiapp.domain.model.CriterioDeAcomodo;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.FranjaDelDia;
import co.edu.ucundinamarca.cundiapp.domain.model.GrupoOfertado;
import co.edu.ucundinamarca.cundiapp.domain.service.ArmadorDeHorario;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * La oferta de matrícula que escribe el estudiante y lo que prefiere (RF13, SCRUM-208). Solo se exige que
 * vengan las materias; que cada pieza sea válida lo comprueba el dominio, para que el mensaje sea el mismo
 * venga de donde venga. Antes de armar los objetos se mira el tamaño, para que un cuerpo enorme no se
 * convierta en millones de objetos solo para rechazarlo.
 *
 * @param asignaturas las materias que quiere matricular, cada una con sus grupos
 * @param criterio lo que prefiere; puede faltar
 */
public record ProponerHorariosDto(@NotNull List<AsignaturaDto> asignaturas, CriterioDto criterio) {

	/** Una materia con sus grupos. */
	public record AsignaturaDto(String nombre, List<GrupoDto> grupos) {
	}

	/** Un grupo con sus bloques. */
	public record GrupoDto(String codigo, List<BloqueDto> bloques) {
	}

	/**
	 * @param dia {@code lunes}, {@code martes}, {@code miercoles}, {@code jueves}, {@code viernes}, {@code sabado}
	 *     o {@code domingo}
	 * @param inicio hora de inicio, por ejemplo {@code 08:00}
	 * @param fin hora de fin, por ejemplo {@code 10:00}
	 */
	public record BloqueDto(String dia, LocalTime inicio, LocalTime fin) {
	}

	/**
	 * @param franja {@code manana}, {@code tarde} o {@code noche}; vacía es sin preferencia
	 * @param pocosHuecos si quiere pocos huecos entre clases; vacío es que no
	 * @param diasLibres los días que quiere sin clase (se ignora si {@code diaLibre} es verdadero)
	 * @param diaLibre si quiere tener un día libre (SCRUM-251): se le ofrece una opción por cada día que se puede
	 *     liberar; vacío es que no
	 */
	public record CriterioDto(String franja, Boolean pocosHuecos, List<String> diasLibres, Boolean diaLibre) {
	}

	public List<AsignaturaOfertada> aOferta() {
		revisarTamano();
		List<AsignaturaOfertada> oferta = new ArrayList<>();
		for (AsignaturaDto asignatura : asignaturas) {
			oferta.add(aAsignatura(asignatura));
		}
		return oferta;
	}

	public CriterioDeAcomodo aCriterio() {
		if (criterio == null) {
			return CriterioDeAcomodo.ninguno();
		}
		FranjaDelDia franjaElegida =
				criterio.franja() == null || criterio.franja().isBlank() ? null : FranjaDelDia.desdeApi(criterio.franja());
		Set<DiaDeLaSemana> libres = new TreeSet<>();
		if (criterio.diasLibres() != null) {
			for (String dia : criterio.diasLibres()) {
				libres.add(DiaDeLaSemana.desdeBd(limpio(dia)));
			}
		}
		return new CriterioDeAcomodo(
				franjaElegida, Boolean.TRUE.equals(criterio.pocosHuecos()), libres, Boolean.TRUE.equals(criterio.diaLibre()));
	}

	private void revisarTamano() {
		if (asignaturas.size() > ArmadorDeHorario.MAXIMO_DE_ASIGNATURAS) {
			throw new ReglaDeNegocioVioladaException(
					"Puedes armar hasta %d materias a la vez".formatted(ArmadorDeHorario.MAXIMO_DE_ASIGNATURAS));
		}
		for (AsignaturaDto asignatura : asignaturas) {
			if (asignatura == null) {
				throw new ReglaDeNegocioVioladaException("Falta una de las materias");
			}
			if (asignatura.grupos() == null) {
				continue;
			}
			if (asignatura.grupos().size() > AsignaturaOfertada.MAXIMO_DE_GRUPOS) {
				throw new ReglaDeNegocioVioladaException(
						"La materia no puede tener más de %d grupos".formatted(AsignaturaOfertada.MAXIMO_DE_GRUPOS));
			}
			for (GrupoDto grupo : asignatura.grupos()) {
				if (grupo != null && grupo.bloques() != null && grupo.bloques().size() > GrupoOfertado.MAXIMO_DE_BLOQUES) {
					throw new ReglaDeNegocioVioladaException(
							"Un grupo no puede tener más de %d bloques".formatted(GrupoOfertado.MAXIMO_DE_BLOQUES));
				}
			}
		}
	}

	private static AsignaturaOfertada aAsignatura(AsignaturaDto asignatura) {
		List<GrupoOfertado> grupos = new ArrayList<>();
		if (asignatura.grupos() != null) {
			for (GrupoDto grupo : asignatura.grupos()) {
				grupos.add(aGrupo(grupo));
			}
		}
		return new AsignaturaOfertada(asignatura.nombre(), grupos);
	}

	/** Un grupo del cuerpo como grupo de dominio; también lo usa el cambio de grupo (SCRUM-245). */
	public static GrupoOfertado aGrupo(GrupoDto grupo) {
		if (grupo == null) {
			throw new ReglaDeNegocioVioladaException("Falta uno de los grupos");
		}
		if (grupo.bloques() != null && grupo.bloques().size() > GrupoOfertado.MAXIMO_DE_BLOQUES) {
			throw new ReglaDeNegocioVioladaException(
					"Un grupo no puede tener más de %d bloques".formatted(GrupoOfertado.MAXIMO_DE_BLOQUES));
		}
		return new GrupoOfertado(grupo.codigo(), aBloques(grupo.bloques()));
	}

	private static List<BloqueDeOferta> aBloques(List<BloqueDto> bloques) {
		List<BloqueDeOferta> resultado = new ArrayList<>();
		if (bloques != null) {
			for (BloqueDto bloque : bloques) {
				if (bloque == null) {
					throw new ReglaDeNegocioVioladaException("Falta uno de los bloques");
				}
				resultado.add(new BloqueDeOferta(DiaDeLaSemana.desdeBd(limpio(bloque.dia())), bloque.inicio(), bloque.fin()));
			}
		}
		return resultado;
	}

	private static String limpio(String dia) {
		return dia == null ? null : dia.strip().toLowerCase(Locale.ROOT);
	}
}
