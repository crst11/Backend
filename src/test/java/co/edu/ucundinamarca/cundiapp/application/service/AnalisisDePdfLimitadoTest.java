package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteDeHorarioPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteHistorialAcademicoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.HistorialAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDePeticionesPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadasPeticionesException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Analizar un PDF cuesta mucho: pasado el cupo del estudiante ni se abre el archivo (SCRUM-88). */
@ExtendWith(MockitoExtension.class)
class AnalisisDePdfLimitadoTest {

	private static final int ESTUDIANTE = 9;

	@Mock
	private LimitadorDePeticionesPort limitador;
	@Mock
	private FuenteHistorialAcademicoPort fuenteDelRegistro;
	@Mock
	private FuenteDeHorarioPort fuenteDelHorario;
	@Mock
	private EstudianteRepositorio estudiantes;
	@Mock
	private ProgramaAcademicoRepositorio programas;
	@Mock
	private HistorialAcademicoRepositorio historiales;
	@Mock
	private ImportacionDeHorarioRepositorio horarios;

	@Test
	void elRegistroExtendidoNoSeLeeSiElEstudianteYaPasoSuCupo() {
		given(limitador.esperaSiSeExcede(eq("analisis-pdf:" + ESTUDIANTE), eq(10), eq(Duration.ofMinutes(10))))
				.willReturn(Optional.of(Duration.ofMinutes(3)));
		var servicio = new AnalizarRegistroExtendidoServicio(fuenteDelRegistro, estudiantes, programas, historiales, limitador);

		assertThatThrownBy(() -> servicio.ejecutar(ESTUDIANTE, new byte[] {1}))
				.isInstanceOf(DemasiadasPeticionesException.class)
				.hasMessageContaining("3 minutos");
		verifyNoInteractions(fuenteDelRegistro, estudiantes);
	}

	@Test
	void elReporteDeHorarioNoSeLeeSiElEstudianteYaPasoSuCupo() {
		given(limitador.esperaSiSeExcede(anyString(), anyInt(), any())).willReturn(Optional.of(Duration.ofSeconds(30)));
		var servicio = new AnalizarReporteDeHorarioServicio(fuenteDelHorario, estudiantes, programas, horarios, limitador);

		assertThatThrownBy(() -> servicio.ejecutar(ESTUDIANTE, new byte[] {1}))
				.isInstanceOf(DemasiadasPeticionesException.class)
				.hasMessageContaining("30 segundos");
		verifyNoInteractions(fuenteDelHorario, estudiantes);
	}

	@Test
	void losDosReportesComparteElMismoCupoDelEstudiante() {
		given(limitador.esperaSiSeExcede(anyString(), anyInt(), any())).willReturn(Optional.of(Duration.ofSeconds(5)));
		var registro = new AnalizarRegistroExtendidoServicio(fuenteDelRegistro, estudiantes, programas, historiales, limitador);
		var horario = new AnalizarReporteDeHorarioServicio(fuenteDelHorario, estudiantes, programas, horarios, limitador);

		assertThatThrownBy(() -> registro.ejecutar(ESTUDIANTE, new byte[] {1})).isInstanceOf(DemasiadasPeticionesException.class);
		assertThatThrownBy(() -> horario.ejecutar(ESTUDIANTE, new byte[] {1})).isInstanceOf(DemasiadasPeticionesException.class);
		verify(limitador, times(2)).esperaSiSeExcede(eq("analisis-pdf:" + ESTUDIANTE), anyInt(), any());
	}
}
