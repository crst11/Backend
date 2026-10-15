package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.AgregarClaseAlHorario.DatosDeLaClase;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio.AsignaturaEnCurso;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Agregar un bloque al horario: lo que el servicio comprueba antes de escribir (SCRUM-25). */
@ExtendWith(MockitoExtension.class)
class AgregarClaseAlHorarioServicioTest {

	private static final int ESTUDIANTE = 5;
	private static final int MATRICULA = 7;

	@Mock
	private HorarioRepositorio horarios;

	private DatosDeLaClase datos(int desde, int hasta) {
		return new DatosDeLaClase(DiaDeLaSemana.LUNES, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), null, null);
	}

	@Test
	void guardaElBloqueConElSiguienteNumeroDeOrdenYDevuelveLaClase() {
		given(horarios.asignaturaEnCurso(ESTUDIANTE, MATRICULA))
				.willReturn(Optional.of(new AsignaturaEnCurso(MATRICULA, "CAD1", "Álgebra Lineal")));
		given(horarios.siguienteConsecutivo(MATRICULA)).willReturn(3);

		var clase = new AgregarClaseAlHorarioServicio(horarios).ejecutar(ESTUDIANTE, MATRICULA, datos(8, 10));

		ArgumentCaptor<BloqueDeHorario> guardado = ArgumentCaptor.forClass(BloqueDeHorario.class);
		verify(horarios).guardar(anyInt(), anyInt(), guardado.capture());
		assertThat(guardado.getValue().consecutivo()).isEqualTo(3);
		assertThat(clase.asignatura()).isEqualTo("Álgebra Lineal");
		assertThat(clase.bloque().horaInicio()).isEqualTo(LocalTime.of(8, 0));
	}

	@Test
	void unaAsignaturaQueNoEstaEnCursoONoEsDelEstudianteSeDiceComoNoEncontrada() {
		given(horarios.asignaturaEnCurso(ESTUDIANTE, MATRICULA)).willReturn(Optional.empty());

		assertThatThrownBy(() -> new AgregarClaseAlHorarioServicio(horarios).ejecutar(ESTUDIANTE, MATRICULA, datos(8, 10)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("estás cursando");

		verify(horarios, never()).guardar(anyInt(), anyInt(), any());
	}

	@Test
	void unBloqueConLasHorasAlRevesNoEscribeNada() {
		given(horarios.asignaturaEnCurso(ESTUDIANTE, MATRICULA))
				.willReturn(Optional.of(new AsignaturaEnCurso(MATRICULA, "CAD1", "Álgebra Lineal")));
		given(horarios.siguienteConsecutivo(MATRICULA)).willReturn(1);

		assertThatThrownBy(() -> new AgregarClaseAlHorarioServicio(horarios).ejecutar(ESTUDIANTE, MATRICULA, datos(10, 8)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);

		verify(horarios, never()).guardar(anyInt(), anyInt(), any());
	}
}
