package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import co.edu.ucundinamarca.cundiapp.application.port.in.CrearMiPendiente.Datos;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.PendienteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDePendiente;
import co.edu.ucundinamarca.cundiapp.domain.model.Pendiente;
import co.edu.ucundinamarca.cundiapp.domain.model.PrioridadDePendiente;
import co.edu.ucundinamarca.cundiapp.domain.model.SituacionDelPendiente;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Crear, listar, marcar y borrar pendientes: lo que los servicios agregan al dominio (RF06, SCRUM-29 y 30). */
@ExtendWith(MockitoExtension.class)
class MisPendientesServiciosTest {

	private static final int ESTUDIANTE = 5;
	private static final Instant AHORA = Instant.parse("2026-10-20T15:00:00Z");
	private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

	@Mock
	private EstructuraDeEvaluacionRepositorio evaluaciones;

	private final Memoria pendientes = new Memoria();

	/** El repositorio en memoria: lo justo para probar los servicios sin base de datos. */
	private static final class Memoria implements PendienteRepositorio {
		final List<Pendiente> guardados = new ArrayList<>();
		int siguiente = 1;

		@Override
		public Pendiente guardar(Pendiente pendiente) {
			Pendiente con = pendiente.id() != null ? pendiente : new Pendiente(siguiente++, pendiente.idEstudiante(),
					pendiente.idMatricula(), pendiente.titulo(), pendiente.descripcion(), pendiente.fechaLimite(),
					pendiente.prioridad(), pendiente.estado(), pendiente.fechaCreacion());
			guardados.removeIf(p -> p.id().equals(con.id()));
			guardados.add(con);
			return con;
		}

		@Override
		public List<Pendiente> listar(int idEstudiante, boolean incluirCumplidos) {
			return guardados.stream().filter(p -> p.idEstudiante() == idEstudiante)
					.filter(p -> incluirCumplidos || p.estado() == EstadoDePendiente.ABIERTO).toList();
		}

		@Override
		public Optional<Pendiente> buscar(int idEstudiante, int idPendiente) {
			return guardados.stream().filter(p -> p.idEstudiante() == idEstudiante && p.id() == idPendiente).findFirst();
		}

		@Override
		public boolean eliminar(int idEstudiante, int idPendiente) {
			return guardados.removeIf(p -> p.idEstudiante() == idEstudiante && p.id() == idPendiente);
		}
	}

	private CrearMiPendienteServicio crear() {
		return new CrearMiPendienteServicio(pendientes, evaluaciones, RELOJ);
	}

	private static Datos datos(String titulo, Duration dentroDe, Integer matricula) {
		return new Datos(titulo, null, AHORA.plus(dentroDe), null, matricula);
	}

	@Test
	void creaUnPendienteConSuNumeroYSuSituacion() {
		var creado = crear().ejecutar(ESTUDIANTE, datos("Taller 3", Duration.ofHours(20), null));

		assertThat(creado.pendiente().id()).isEqualTo(1);
		assertThat(creado.pendiente().prioridad()).isEqualTo(PrioridadDePendiente.MEDIA);
		assertThat(creado.situacion()).isEqualTo(SituacionDelPendiente.URGENTE);
		assertThat(pendientes.guardados).hasSize(1);
	}

	@Test
	void unPendienteMalEscritoSeRechazaSinTocarNada() {
		assertThatThrownBy(() -> crear().ejecutar(ESTUDIANTE, datos("  ", Duration.ofDays(1), null)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);

		assertThat(pendientes.guardados).isEmpty();
		verifyNoInteractions(evaluaciones);
	}

	@Test
	void conAsignaturaPropiaLoAsocia() {
		given(evaluaciones.matricula(ESTUDIANTE, 7)).willReturn(Optional.of(
				new AsignaturaMatriculada(7, "CAD1", "Álgebra", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 0, 0)));

		var creado = crear().ejecutar(ESTUDIANTE, datos("Parcial", Duration.ofDays(5), 7));

		assertThat(creado.pendiente().idMatricula()).isEqualTo(7);
	}

	@Test
	void conUnaAsignaturaAjenaSeRechazaYNoSeGuardaNada() {
		given(evaluaciones.matricula(ESTUDIANTE, 99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> crear().ejecutar(ESTUDIANTE, datos("Parcial", Duration.ofDays(5), 99)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("matrículas");

		assertThat(pendientes.guardados).isEmpty();
	}

	@Test
	void listaSoloLosMiosOrdenadosYSinLosCumplidosSalvoQueSePidan() {
		crear().ejecutar(ESTUDIANTE, datos("Lejano", Duration.ofDays(9), null));
		crear().ejecutar(ESTUDIANTE, datos("Urgente", Duration.ofHours(3), null));
		crear().ejecutar(ESTUDIANTE, datos("Hecho", Duration.ofHours(1), null));
		crear().ejecutar(99, datos("De otro", Duration.ofHours(1), null));
		new CambiarEstadoDeMiPendienteServicio(pendientes, RELOJ).ejecutar(ESTUDIANTE, 3, true);
		var servicio = new ListarMisPendientesServicio(pendientes, RELOJ);

		assertThat(servicio.ejecutar(ESTUDIANTE, false)).extracting(p -> p.pendiente().titulo())
				.containsExactly("Urgente", "Lejano");
		assertThat(servicio.ejecutar(ESTUDIANTE, true)).extracting(p -> p.pendiente().titulo())
				.containsExactly("Urgente", "Lejano", "Hecho");
	}

	@Test
	void marcaComoHechoYSePuedeReabrir() {
		crear().ejecutar(ESTUDIANTE, datos("Taller", Duration.ofDays(1), null));
		var servicio = new CambiarEstadoDeMiPendienteServicio(pendientes, RELOJ);

		assertThat(servicio.ejecutar(ESTUDIANTE, 1, true).situacion()).isEqualTo(SituacionDelPendiente.CUMPLIDO);
		assertThat(servicio.ejecutar(ESTUDIANTE, 1, false).situacion()).isEqualTo(SituacionDelPendiente.URGENTE);
	}

	@Test
	void marcarUnPendienteAjenoOInexistenteDiceQueNoLoEncuentra() {
		crear().ejecutar(99, datos("De otro", Duration.ofDays(1), null));
		var servicio = new CambiarEstadoDeMiPendienteServicio(pendientes, RELOJ);

		assertThatThrownBy(() -> servicio.ejecutar(ESTUDIANTE, 1, true))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("No encontramos");
		assertThatThrownBy(() -> servicio.ejecutar(ESTUDIANTE, 55, true))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThat(pendientes.guardados.getFirst().estado()).isEqualTo(EstadoDePendiente.ABIERTO);
	}

	@Test
	void borraElMioYNoBorraElDeOtro() {
		crear().ejecutar(ESTUDIANTE, datos("Mío", Duration.ofDays(1), null));
		crear().ejecutar(99, datos("De otro", Duration.ofDays(1), null));
		var servicio = new EliminarMiPendienteServicio(pendientes);

		servicio.ejecutar(ESTUDIANTE, 1);

		assertThat(pendientes.guardados).extracting(Pendiente::titulo).containsExactly("De otro");
		assertThatThrownBy(() -> servicio.ejecutar(ESTUDIANTE, 2))
				.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("No encontramos");
		assertThat(pendientes.guardados).hasSize(1);
	}
}
