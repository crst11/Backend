package co.edu.ucundinamarca.cundiapp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeOferta;
import co.edu.ucundinamarca.cundiapp.domain.model.ChoqueDelGrupo;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.EleccionDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.GrupoOfertado;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDelCandidato;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * El cambio de grupo de una materia del horario armado (RF13, SCRUM-245): cada grupo candidato cabe o dice con
 * qué clase de otra materia choca. Es dominio puro: sin base de datos ni HTTP.
 */
class VerificadorDeCambioDeGrupoTest {

	private final VerificadorDeCambioDeGrupo verificador = new VerificadorDeCambioDeGrupo();

	private static BloqueDeOferta bloque(DiaDeLaSemana dia, String inicio, String fin) {
		return new BloqueDeOferta(dia, LocalTime.parse(inicio), LocalTime.parse(fin));
	}

	private static GrupoOfertado grupo(String codigo, BloqueDeOferta... bloques) {
		return new GrupoOfertado(codigo, List.of(bloques));
	}

	private static EleccionDeHorario eleccion(String asignatura, GrupoOfertado grupo) {
		return new EleccionDeHorario(asignatura, grupo);
	}

	private final List<EleccionDeHorario> horario = List.of(
			eleccion("ÁLGEBRA LINEAL", grupo("502", bloque(DiaDeLaSemana.LUNES, "07:00", "09:00"))),
			eleccion("CÁLCULO", grupo("501", bloque(DiaDeLaSemana.MARTES, "09:00", "11:00"))));

	@Test
	void unCandidatoSinChoquesCabe() {
		var resultado = verificador.verificar(horario, "FÍSICA",
				List.of(grupo("501", bloque(DiaDeLaSemana.MIERCOLES, "07:00", "09:00"))));

		assertThat(resultado).containsExactly(new ResultadoDelCandidato("501", true, List.of()));
	}

	@Test
	void unCandidatoQueChocaDiceConCualMateriaGrupoDiaYHora() {
		var resultado = verificador.verificar(horario, "FÍSICA",
				List.of(grupo("503", bloque(DiaDeLaSemana.LUNES, "08:00", "10:00"))));

		assertThat(resultado).singleElement().satisfies(r -> {
			assertThat(r.grupo()).isEqualTo("503");
			assertThat(r.cabe()).isFalse();
			assertThat(r.choques()).containsExactly(new ChoqueDelGrupo(
					"ÁLGEBRA LINEAL", "502", DiaDeLaSemana.LUNES, LocalTime.of(7, 0), LocalTime.of(9, 0)));
		});
	}

	@Test
	void terminarCuandoEmpiezaOtraClaseNoEsChoque() {
		var resultado = verificador.verificar(horario, "FÍSICA",
				List.of(grupo("501", bloque(DiaDeLaSemana.LUNES, "09:00", "11:00"))));

		assertThat(resultado.getFirst().cabe()).isTrue();
	}

	@Test
	void variosCandidatosSeEvaluanPorSeparadoYEnElOrdenQueLlegan() {
		var resultado = verificador.verificar(horario, "FÍSICA", List.of(
				grupo("501", bloque(DiaDeLaSemana.LUNES, "07:00", "09:00")),
				grupo("502", bloque(DiaDeLaSemana.JUEVES, "07:00", "09:00")),
				grupo("503", bloque(DiaDeLaSemana.MARTES, "10:00", "12:00"))));

		assertThat(resultado).extracting(ResultadoDelCandidato::grupo).containsExactly("501", "502", "503");
		assertThat(resultado).extracting(ResultadoDelCandidato::cabe).containsExactly(false, true, false);
	}

	@Test
	void chocaConMasDeUnaMateriaALaVez() {
		var resultado = verificador.verificar(horario, "FÍSICA", List.of(grupo("501",
				bloque(DiaDeLaSemana.LUNES, "08:00", "09:00"), bloque(DiaDeLaSemana.MARTES, "10:00", "11:00"))));

		assertThat(resultado.getFirst().choques()).extracting(ChoqueDelGrupo::asignatura)
				.containsExactly("ÁLGEBRA LINEAL", "CÁLCULO");
	}

	@Test
	void ignoraLaEleccionDeLaMateriaQueSeCambia() {
		// 502 de álgebra lineal está el lunes de 7 a 9: cambiarla por 501, que es el mismo horario, no choca consigo.
		var resultado = verificador.verificar(horario, "ÁLGEBRA LINEAL",
				List.of(grupo("501", bloque(DiaDeLaSemana.LUNES, "07:00", "09:00"))));

		assertThat(resultado).containsExactly(new ResultadoDelCandidato("501", true, List.of()));
	}

	@Test
	void elNombreDeLaMateriaSeComparaSinImportarMayusculasNiEspacios() {
		var resultado = verificador.verificar(horario, "  álgebra lineal ",
				List.of(grupo("501", bloque(DiaDeLaSemana.LUNES, "07:00", "09:00"))));

		assertThat(resultado.getFirst().cabe()).isTrue();
	}

	@Test
	void unHorarioVacioHaceQueTodoCaiga() {
		var resultado = verificador.verificar(List.of(), "FÍSICA", List.of(
				grupo("501", bloque(DiaDeLaSemana.LUNES, "07:00", "09:00")),
				grupo("502", bloque(DiaDeLaSemana.LUNES, "07:00", "09:00"))));

		assertThat(resultado).extracting(ResultadoDelCandidato::cabe).containsExactly(true, true);
	}

	@Test
	void sinMateriaACambiarOSinCandidatosSeRechaza() {
		var candidato = grupo("501", bloque(DiaDeLaSemana.LUNES, "07:00", "09:00"));

		assertThatThrownBy(() -> verificador.verificar(horario, " ", List.of(candidato)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> verificador.verificar(horario, "FÍSICA", List.of()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void masEleccionesOCandidatosDelMaximoSeRechaza() {
		var unBloque = bloque(DiaDeLaSemana.LUNES, "07:00", "08:00");
		var demasiadasElecciones = IntStream.rangeClosed(0, VerificadorDeCambioDeGrupo.MAXIMO_DE_ELECCIONES)
				.mapToObj(i -> eleccion("MATERIA " + i, grupo("1", unBloque)))
				.toList();
		var demasiadosCandidatos = IntStream.rangeClosed(0, VerificadorDeCambioDeGrupo.MAXIMO_DE_CANDIDATOS)
				.mapToObj(i -> grupo("G" + i, unBloque))
				.toList();

		assertThatThrownBy(() -> verificador.verificar(demasiadasElecciones, "FÍSICA", List.of(grupo("1", unBloque))))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> verificador.verificar(horario, "FÍSICA", demasiadosCandidatos))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}
}
