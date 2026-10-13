package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.EspacioFisico;
import co.edu.ucundinamarca.cundiapp.domain.model.FiltroDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.OrdenDelHorario;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Consultar el horario filtrado: lo que se puede elegir sale del horario completo (SCRUM-26). */
@ExtendWith(MockitoExtension.class)
class ConsultarMiHorarioServicioTest {

	@Mock
	private HorarioRepositorio horarios;

	private static ClaseDelHorario clase(int matricula, String codigo, String asignatura, String sede, int desde) {
		return new ClaseDelHorario(matricula, codigo, asignatura, new BloqueDeHorario(1, DiaDeLaSemana.LUNES,
				LocalTime.of(desde, 0), LocalTime.of(desde + 2, 0), new EspacioFisico("F-1", null, sede), null));
	}

	@Test
	void lasOpcionesSalenDelHorarioCompletoYNoDelFiltrado() {
		given(horarios.clasesDe(5)).willReturn(List.of(
				clase(1, "ALG", "Álgebra", "Fusagasugá", 8),
				clase(2, "FIS", "Física", "Soacha", 10)));

		var resultado = new ConsultarMiHorarioServicio(horarios)
				.ejecutar(5, new FiltroDeHorario(null, "Soacha"), OrdenDelHorario.HORA);

		assertThat(resultado.horario().totalDeClases()).isEqualTo(1);
		assertThat(resultado.sedes()).containsExactly("Fusagasugá", "Soacha");
		assertThat(resultado.asignaturas()).hasSize(2);
		assertThat(resultado.filtro().sede()).isEqualTo("Soacha");
		assertThat(resultado.orden()).isEqualTo(OrdenDelHorario.HORA);
	}

	@Test
	void sinClasesTodoVieneVacioPeroNoFalla() {
		given(horarios.clasesDe(5)).willReturn(List.of());

		var resultado = new ConsultarMiHorarioServicio(horarios)
				.ejecutar(5, FiltroDeHorario.SIN_FILTRO, OrdenDelHorario.ASIGNATURA);

		assertThat(resultado.horario().estaVacio()).isTrue();
		assertThat(resultado.sedes()).isEmpty();
		assertThat(resultado.asignaturas()).isEmpty();
	}
}
