package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort.TokenDeAcceso;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort.IdentidadExterna;
import co.edu.ucundinamarca.cundiapp.application.port.out.VinculoConGoogleRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.GoogleNoVinculadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.IdentidadExternaInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MetodoDeAcceso;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IniciarSesionConGoogleServicioTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");
	private static final OrigenDeSesion ORIGEN = new OrigenDeSesion("10.0.0.1", "Chrome");
	private static final IdentidadExterna ANA = new IdentidadExterna("1098765", "ana.diaz@gmail.com", true);

	private final VerificadorDeIdentidadExternaPort verificador = mock(VerificadorDeIdentidadExternaPort.class);
	private final VinculoConGoogleRepositorio vinculos = mock(VinculoConGoogleRepositorio.class);
	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final EmisorDeTokensPort tokens = mock(EmisorDeTokensPort.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private IniciarSesionConGoogleServicio servicio;

	private Estudiante cuenta(EstadoCuenta estado) {
		return new Estudiante(7, "Ana", "Díaz", new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co"), estado, true, AHORA);
	}

	@BeforeEach
	void configurar() {
		var abridor = new AbridorDeSesion(sesiones, tokens, Duration.ofMinutes(20), Duration.ofDays(7));
		servicio = new IniciarSesionConGoogleServicio(verificador, vinculos, estudiantes, abridor, reloj);
		given(reloj.ahora()).willReturn(AHORA);
		given(verificador.verificar("id-token")).willReturn(ANA);
		given(vinculos.estudianteVinculadoA("1098765")).willReturn(Optional.of(7));
		given(estudiantes.buscarPorId(7)).willReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA)));
		given(tokens.generarTokenDeRefresco()).willReturn("refresco-123");
		given(tokens.emitirAcceso(any(), any(), any())).willReturn(new TokenDeAcceso("jwt", AHORA.plus(Duration.ofMinutes(20))));
	}

	@Test
	void entraALaCuentaQueVinculoEsaCuentaDeGoogleYMarcaLaSesionComoDeGoogle() {
		var sesion = servicio.ejecutar("id-token", ORIGEN);

		assertThat(sesion.estudiante().id()).isEqualTo(7);
		assertThat(sesion.tokenDeAcceso()).isEqualTo("jwt");
		var guardada = ArgumentCaptor.forClass(Sesion.class);
		verify(sesiones).guardar(guardada.capture());
		assertThat(guardada.getValue().metodo()).isEqualTo(MetodoDeAcceso.GOOGLE);
		verify(vinculos).registrarAcceso(7, AHORA);
	}

	@Test
	void unaCuentaDeGoogleSinVincularNoEntraAunqueElCorreoCoincida() {
		given(vinculos.estudianteVinculadoA("1098765")).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.ejecutar("id-token", ORIGEN)).isInstanceOf(GoogleNoVinculadoException.class);
		verify(sesiones, never()).guardar(any());
	}

	@Test
	void unTokenInvalidoNoAbreSesion() {
		given(verificador.verificar("malo")).willThrow(new IdentidadExternaInvalidaException());

		assertThatThrownBy(() -> servicio.ejecutar("malo", ORIGEN)).isInstanceOf(IdentidadExternaInvalidaException.class);
		verify(sesiones, never()).guardar(any());
	}

	@Test
	void unaCuentaInactivaNoEntraConGoogle() {
		given(estudiantes.buscarPorId(7)).willReturn(Optional.of(cuenta(EstadoCuenta.INACTIVA)));

		assertThatThrownBy(() -> servicio.ejecutar("id-token", ORIGEN)).isInstanceOf(CuentaNoActivaException.class);
		verify(sesiones, never()).guardar(any());
	}
}
