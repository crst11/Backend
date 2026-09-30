package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort.IdentidadExterna;
import co.edu.ucundinamarca.cundiapp.application.port.out.VinculoConGoogleRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.GoogleYaVinculadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Criterios de SCRUM-48: solo con el correo verificado y una cuenta de Google por cuenta de CundiApp. */
class VincularGoogleServicioTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final VerificadorDeIdentidadExternaPort verificador = mock(VerificadorDeIdentidadExternaPort.class);
	private final VinculoConGoogleRepositorio vinculos = mock(VinculoConGoogleRepositorio.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private final VincularGoogleServicio servicio = new VincularGoogleServicio(estudiantes, verificador, vinculos, reloj);

	private Estudiante cuenta(EstadoCuenta estado) {
		return new Estudiante(7, "Ana", "Díaz", new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co"), estado, true, AHORA);
	}

	@BeforeEach
	void configurar() {
		given(reloj.ahora()).willReturn(AHORA);
		given(estudiantes.buscarPorId(7)).willReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA)));
		given(verificador.verificar("id-token")).willReturn(new IdentidadExterna("1098765", "ana.diaz@gmail.com", true));
		given(vinculos.estudianteVinculadoA("1098765")).willReturn(Optional.empty());
		given(vinculos.buscarDe(7)).willReturn(Optional.empty());
	}

	@Test
	void vinculaLaCuentaDeGoogleConSuCorreoYLaFecha() {
		var vinculo = servicio.ejecutar(7, "id-token");

		assertThat(vinculo).isEqualTo(new VinculoConGoogle("1098765", "ana.diaz@gmail.com", AHORA));
		verify(vinculos).vincular(7, vinculo);
	}

	@Test
	void conElCorreoInstitucionalSinVerificarNoSePuedeVincular() {
		given(estudiantes.buscarPorId(7)).willReturn(Optional.of(cuenta(EstadoCuenta.PENDIENTE)));

		assertThatThrownBy(() -> servicio.ejecutar(7, "id-token"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Verifica tu correo institucional");
		verify(verificador, never()).verificar(anyString());
	}

	@Test
	void unaCuentaDeGoogleNoQuedaVinculadaADosCuentas() {
		given(vinculos.estudianteVinculadoA("1098765")).willReturn(Optional.of(99));

		assertThatThrownBy(() -> servicio.ejecutar(7, "id-token"))
				.isInstanceOf(GoogleYaVinculadoException.class)
				.hasMessageContaining("otra cuenta de CundiApp");
		verify(vinculos, never()).vincular(anyInt(), any());
	}

	@Test
	void siYaTieneOtraCuentaDeGoogleDebeDesvincularlaPrimero() {
		given(vinculos.buscarDe(7)).willReturn(Optional.of(new VinculoConGoogle("otro-sub", "ana.personal@gmail.com", AHORA)));

		assertThatThrownBy(() -> servicio.ejecutar(7, "id-token"))
				.isInstanceOf(GoogleYaVinculadoException.class)
				.hasMessageContaining("Desvincúlala primero");
	}

	@Test
	void vincularOtraVezLaMismaCuentaDeGoogleNoCambiaNada() {
		var existente = new VinculoConGoogle("1098765", "ana.diaz@gmail.com", AHORA.minusSeconds(3600));
		given(vinculos.estudianteVinculadoA("1098765")).willReturn(Optional.of(7));
		given(vinculos.buscarDe(7)).willReturn(Optional.of(existente));

		assertThat(servicio.ejecutar(7, "id-token")).isEqualTo(existente);
		verify(vinculos, never()).vincular(anyInt(), any());
	}

	@Test
	void noVinculaUnaCuentaDeGoogleCuyoCorreoNoEstaVerificado() {
		given(verificador.verificar("id-token")).willReturn(new IdentidadExterna("1098765", "ana.diaz@gmail.com", false));

		assertThatThrownBy(() -> servicio.ejecutar(7, "id-token")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		verify(vinculos, never()).vincular(anyInt(), any());
	}

	@Test
	void consultarYDesvincularDelegan() {
		var vinculo = new VinculoConGoogle("1098765", "ana.diaz@gmail.com", AHORA);
		given(vinculos.buscarDe(7)).willReturn(Optional.of(vinculo));

		assertThat(new ConsultarVinculoConGoogleServicio(vinculos).ejecutar(7)).contains(vinculo);
		new DesvincularGoogleServicio(vinculos).ejecutar(7);
		verify(vinculos).desvincular(7);
	}
}
