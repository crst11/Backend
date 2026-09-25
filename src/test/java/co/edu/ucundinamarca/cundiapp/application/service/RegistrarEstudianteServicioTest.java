package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.DatosDeRegistro;
import co.edu.ucundinamarca.cundiapp.application.port.out.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoYaRegistradoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Orquestación probada con dobles de los puertos: sin Spring, sin base de datos. */
class RegistrarEstudianteServicioTest {

	private final EstudianteRepositorio repositorio = mock(EstudianteRepositorio.class);
	private final CifradorDeContrasenaPort cifrador = mock(CifradorDeContrasenaPort.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private final EmisorDeCodigoDeVerificacion emisor = mock(EmisorDeCodigoDeVerificacion.class);
	private RegistrarEstudianteServicio servicio;

	@BeforeEach
	void configurar() {
		servicio = new RegistrarEstudianteServicio(repositorio, cifrador, reloj, emisor);
		given(reloj.ahora()).willReturn(Instant.parse("2026-01-15T10:00:00Z"));
	}

	@Test
	void registraUnaCuentaPendienteYCifraLaContrasena() {
		given(repositorio.existeCuentaCon(any())).willReturn(false);
		given(cifrador.cifrar("unaClaveSegura")).willReturn("hash-simulado");
		given(repositorio.guardarConCredencialLocal(any(), any())).willAnswer(inv ->
				new Estudiante(1, "Ana", "Díaz",
						inv.getArgument(0, Estudiante.class).correo(),
						EstadoCuenta.PENDIENTE, true, Instant.parse("2026-01-15T10:00:00Z")));

		var datos = new DatosDeRegistro("ana.diaz@ucundinamarca.edu.co", "unaClaveSegura", "Ana", "Díaz", true);
		Estudiante registrado = servicio.ejecutar(datos);

		assertThat(registrado.id()).isEqualTo(1);
		assertThat(registrado.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
		verify(repositorio).guardarConCredencialLocal(any(), org.mockito.ArgumentMatchers.eq("hash-simulado"));
		verify(emisor).emitirYEnviar(registrado);
	}

	@Test
	void rechazaUnCorreoQueYaTieneCuenta() {
		given(repositorio.existeCuentaCon(new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co"))).willReturn(true);

		var datos = new DatosDeRegistro("ana.diaz@ucundinamarca.edu.co", "unaClaveSegura", "Ana", "Díaz", true);

		assertThatThrownBy(() -> servicio.ejecutar(datos)).isInstanceOf(CorreoYaRegistradoException.class);
		verify(repositorio, never()).guardarConCredencialLocal(any(), any());
		verify(emisor, never()).emitirYEnviar(any());
	}

	@Test
	void rechazaUnCorreoQueNoEsInstitucionalAntesDeConsultarElRepositorio() {
		var datos = new DatosDeRegistro("ana.diaz@gmail.com", "unaClaveSegura", "Ana", "Díaz", true);

		assertThatThrownBy(() -> servicio.ejecutar(datos)).isInstanceOf(ReglaDeNegocioVioladaException.class);
		verify(repositorio, never()).existeCuentaCon(any());
	}
}
