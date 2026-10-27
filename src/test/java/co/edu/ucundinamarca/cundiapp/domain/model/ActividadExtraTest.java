package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/** Las reglas de una actividad extra: algo con día y hora que no es una asignatura (SCRUM-266). */
class ActividadExtraTest {

	private static final LocalTime CINCO = LocalTime.of(17, 0);
	private static final LocalTime SIETE = LocalTime.of(19, 0);

	private static ActividadExtra baile(String nombre, LocalTime inicio, LocalTime fin, String lugar, String responsable) {
		return ActividadExtra.nueva(7, nombre, DiaDeLaSemana.LUNES, inicio, fin, lugar, responsable);
	}

	@Test
	void unaActividadCompletaSeGuardaTalCual() {
		var actividad = baile("Baile", CINCO, SIETE, "Coliseo", "Profe Ana");

		assertThat(actividad.id()).isNull();
		assertThat(actividad.idEstudiante()).isEqualTo(7);
		assertThat(actividad.nombre()).isEqualTo("Baile");
		assertThat(actividad.dia()).isEqualTo(DiaDeLaSemana.LUNES);
		assertThat(actividad.lugar()).isEqualTo("Coliseo");
		assertThat(actividad.responsable()).isEqualTo("Profe Ana");
	}

	@Test
	void elNombreSeRecortaYEsObligatorio() {
		assertThat(baile("  Semillero de IA ", CINCO, SIETE, null, null).nombre()).isEqualTo("Semillero de IA");

		assertThatThrownBy(() -> baile("   ", CINCO, SIETE, null, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessage("Escribe el nombre de la actividad");
		assertThatThrownBy(() -> baile(null, CINCO, SIETE, null, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessage("Escribe el nombre de la actividad");
	}

	@Test
	void elNombreNoPasaDeOchentaCaracteres() {
		assertThat(baile("a".repeat(80), CINCO, SIETE, null, null).nombre()).hasSize(80);

		assertThatThrownBy(() -> baile("a".repeat(81), CINCO, SIETE, null, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("80");
	}

	@Test
	void elLugarYElResponsableSonOpcionalesYSeRecortan() {
		var sin = baile("Fútbol", CINCO, SIETE, "   ", "");
		assertThat(sin.lugar()).isNull();
		assertThat(sin.responsable()).isNull();

		var con = baile("Fútbol", CINCO, SIETE, "  Cancha 2 ", " Profe Luis  ");
		assertThat(con.lugar()).isEqualTo("Cancha 2");
		assertThat(con.responsable()).isEqualTo("Profe Luis");
	}

	@Test
	void elLugarYElResponsableNoPasanDeOchentaCaracteres() {
		assertThatThrownBy(() -> baile("Baile", CINCO, SIETE, "l".repeat(81), null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("lugar");
		assertThatThrownBy(() -> baile("Baile", CINCO, SIETE, null, "r".repeat(81)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("responsable");
	}

	@Test
	void laHoraDeInicioTieneQueSerAnteriorALaDeFin() {
		assertThatThrownBy(() -> baile("Baile", SIETE, CINCO, null, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("empezar antes de terminar");
		assertThatThrownBy(() -> baile("Baile", CINCO, CINCO, null, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void faltanElDiaOLasHoras() {
		assertThatThrownBy(() -> ActividadExtra.nueva(7, "Baile", null, CINCO, SIETE, null, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("día");
		assertThatThrownBy(() -> ActividadExtra.nueva(7, "Baile", DiaDeLaSemana.LUNES, null, SIETE, null, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("hora");
	}

	@Test
	void lasHorasSeGuardanAlMinuto() {
		var actividad = baile("Baile", LocalTime.of(17, 0, 30), LocalTime.of(19, 0, 45), null, null);

		assertThat(actividad.horaInicio()).isEqualTo(LocalTime.of(17, 0));
		assertThat(actividad.horaFin()).isEqualTo(LocalTime.of(19, 0));
	}

	@Test
	void comoBloqueSeLeeIgualQueUnaClaseSinSalonNiDocente() {
		var bloque = baile("Baile", CINCO, SIETE, "Coliseo", "Profe Ana").comoBloque();

		assertThat(bloque.consecutivo()).isEqualTo(1);
		assertThat(bloque.dia()).isEqualTo(DiaDeLaSemana.LUNES);
		assertThat(bloque.horaInicio()).isEqualTo(CINCO);
		assertThat(bloque.horaFin()).isEqualTo(SIETE);
		assertThat(bloque.espacio()).isEmpty();
		assertThat(bloque.docente()).isEmpty();
	}

	@Test
	void sePuedeTenerComoMaximoVeinteActividadesExtra() {
		assertThat(ActividadExtra.MAXIMO_POR_ESTUDIANTE).isEqualTo(20);
	}
}
