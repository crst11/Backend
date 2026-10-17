package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Qué es un pendiente y cuándo se le avisa al estudiante que se le viene encima (RF06, SCRUM-29 y SCRUM-30). */
class PendienteTest {

	private static final Instant AHORA = Instant.parse("2026-10-20T15:00:00Z");

	private static Pendiente con(String titulo, Instant fechaLimite) {
		return Pendiente.nuevo(7, null, titulo, null, fechaLimite, null, AHORA);
	}

	@Test
	void sinPrioridadQuedaMedia() {
		assertThat(con("Taller", AHORA.plusSeconds(3600)).prioridad()).isEqualTo(PrioridadDePendiente.MEDIA);
	}

	@Test
	void nuevoQuedaAbiertoYSinNumeroTodavia() {
		var pendiente = con("Taller", AHORA.plusSeconds(3600));

		assertThat(pendiente.estado()).isEqualTo(EstadoDePendiente.ABIERTO);
		assertThat(pendiente.id()).isNull();
		assertThat(pendiente.fechaCreacion()).isEqualTo(AHORA);
	}

	@Test
	void elTituloSeRecortaYEsObligatorio() {
		assertThat(con("  Taller 3  ", AHORA.plusSeconds(60)).titulo()).isEqualTo("Taller 3");
		assertThatThrownBy(() -> con("   ", AHORA.plusSeconds(60)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("título");
		assertThatThrownBy(() -> con(null, AHORA.plusSeconds(60)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void elTituloNoPasaDe120YLaDescripcionNoPasaDe300() {
		assertThatThrownBy(() -> con("a".repeat(121), AHORA.plusSeconds(60)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("120");
		assertThat(con("a".repeat(120), AHORA.plusSeconds(60)).titulo()).hasSize(120);
		assertThatThrownBy(() -> Pendiente.nuevo(7, null, "x", "d".repeat(301), AHORA, null, AHORA))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("300");
	}

	@Test
	void laDescripcionEnBlancoQuedaNula() {
		assertThat(Pendiente.nuevo(7, null, "x", "   ", AHORA, null, AHORA).descripcion()).isNull();
	}

	@Test
	void sinFechaLimiteNoHayPendiente() {
		assertThatThrownBy(() -> con("Taller", null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("fecha");
	}

	@Test
	void laAsignaturaEsOpcional() {
		assertThat(con("Taller", AHORA).idMatricula()).isNull();
		assertThat(Pendiente.nuevo(7, 12, "Taller", null, AHORA, null, AHORA).idMatricula()).isEqualTo(12);
	}

	@Test
	void venceEnMenosDe48HorasEsUrgente() {
		var casi = con("Parcial", AHORA.plus(Duration.ofHours(48)).minusSeconds(1));

		assertThat(casi.situacionA(AHORA)).isEqualTo(SituacionDelPendiente.URGENTE);
	}

	@Test
	void justoEn48HorasYaNoEsUrgente() {
		var justo = con("Parcial", AHORA.plus(Duration.ofHours(48)));

		assertThat(justo.situacionA(AHORA)).isEqualTo(SituacionDelPendiente.PROXIMO);
	}

	@Test
	void conMasTiempoEsProximo() {
		assertThat(con("Proyecto", AHORA.plus(Duration.ofDays(10))).situacionA(AHORA))
				.isEqualTo(SituacionDelPendiente.PROXIMO);
	}

	@Test
	void sePasoLaFechaEstaVencidoYLaHoraExactaTambien() {
		assertThat(con("Taller", AHORA.minusSeconds(1)).situacionA(AHORA)).isEqualTo(SituacionDelPendiente.VENCIDO);
		assertThat(con("Taller", AHORA).situacionA(AHORA)).isEqualTo(SituacionDelPendiente.VENCIDO);
	}

	@Test
	void unoCumplidoNuncaEstaVencidoNiUrgente() {
		var cumplido = con("Taller", AHORA.minus(Duration.ofDays(3))).cumplir();

		assertThat(cumplido.estado()).isEqualTo(EstadoDePendiente.CUMPLIDO);
		assertThat(cumplido.situacionA(AHORA)).isEqualTo(SituacionDelPendiente.CUMPLIDO);
	}

	@Test
	void sePuedeReabrirUnoCumplido() {
		var reabierto = con("Taller", AHORA.plusSeconds(60)).cumplir().reabrir();

		assertThat(reabierto.estado()).isEqualTo(EstadoDePendiente.ABIERTO);
	}

	@Test
	void cumplirNoCambiaElOriginal() {
		var original = con("Taller", AHORA.plusSeconds(60));
		original.cumplir();

		assertThat(original.estado()).isEqualTo(EstadoDePendiente.ABIERTO);
	}

	@Test
	void ordenaDelMasProximoAlMasLejanoConLosVencidosPrimeroYLosCumplidosAlFinal() {
		var lejano = con("Lejano", AHORA.plus(Duration.ofDays(9)));
		var urgente = con("Urgente", AHORA.plus(Duration.ofHours(5)));
		var vencido = con("Vencido", AHORA.minus(Duration.ofDays(2)));
		var hecho = con("Hecho", AHORA.plus(Duration.ofHours(1))).cumplir();
		var medio = con("Medio", AHORA.plus(Duration.ofDays(3)));

		var orden = PendienteConSituacion.ordenar(List.of(lejano, hecho, medio, vencido, urgente), AHORA);

		assertThat(orden).extracting(p -> p.pendiente().titulo())
				.containsExactly("Vencido", "Urgente", "Medio", "Lejano", "Hecho");
		assertThat(orden).extracting(PendienteConSituacion::situacion).containsExactly(
				SituacionDelPendiente.VENCIDO, SituacionDelPendiente.URGENTE, SituacionDelPendiente.PROXIMO,
				SituacionDelPendiente.PROXIMO, SituacionDelPendiente.CUMPLIDO);
	}

	@Test
	void conLaMismaFechaManda_laPrioridadMasAlta() {
		var baja = Pendiente.nuevo(7, null, "Baja", null, AHORA.plusSeconds(600), PrioridadDePendiente.BAJA, AHORA);
		var alta = Pendiente.nuevo(7, null, "Alta", null, AHORA.plusSeconds(600), PrioridadDePendiente.ALTA, AHORA);

		var orden = PendienteConSituacion.ordenar(List.of(baja, alta), AHORA);

		assertThat(orden).extracting(p -> p.pendiente().titulo()).containsExactly("Alta", "Baja");
	}
}
