package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;
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
import java.util.Optional;
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
		given(repositorio.buscarPorCorreo(any())).willReturn(Optional.empty());
		given(cifrador.cifrar("UnaClaveSegura1!")).willReturn("hash-simulado");
		given(repositorio.guardarConCredencialLocal(any(), any())).willAnswer(inv ->
				new Estudiante(1, "Ana", "Díaz",
						inv.getArgument(0, Estudiante.class).correo(),
						EstadoCuenta.PENDIENTE, true, Instant.parse("2026-01-15T10:00:00Z")));

		var datos = new DatosDeRegistro("ana.diaz@ucundinamarca.edu.co", "UnaClaveSegura1!", "Ana", "Díaz", true);
		Estudiante registrado = servicio.ejecutar(datos);

		assertThat(registrado.id()).isEqualTo(1);
		assertThat(registrado.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
		verify(repositorio).guardarConCredencialLocal(any(), org.mockito.ArgumentMatchers.eq("hash-simulado"));
		verify(emisor).emitirYEnviar(registrado, PropositoDelCodigo.VERIFICAR_CORREO);
	}

	@Test
	void rechazaUnaContrasenaDebilSinTocarLaBaseDeDatos() {
		var datos = new DatosDeRegistro("ana.diaz@ucundinamarca.edu.co", "clave123", "Ana", "Díaz", true);

		assertThatThrownBy(() -> servicio.ejecutar(datos))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);

		verify(repositorio, never()).buscarPorCorreo(any());
		verify(repositorio, never()).guardarConCredencialLocal(any(), any());
	}

	@Test
	void elRegistroNoEsperaAlServidorDeCorreo() {
		// El envío sale en segundo plano (SCRUM-67): el registro solo lo encarga y responde.
		given(repositorio.buscarPorCorreo(any())).willReturn(Optional.empty());
		given(repositorio.guardarConCredencialLocal(any(), any())).willAnswer(inv -> inv.getArgument(0, Estudiante.class));

		var datos = new DatosDeRegistro("ana.diaz@ucundinamarca.edu.co", "UnaClaveSegura1!", "Ana", "Díaz", true);
		Estudiante registrado = servicio.ejecutar(datos);

		assertThat(registrado.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
		verify(emisor).emitirYEnviar(registrado, PropositoDelCodigo.VERIFICAR_CORREO);
	}

	@Test
	void rechazaUnCorreoQueYaTieneCuentaActiva() {
		var correo = new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co");
		given(repositorio.buscarPorCorreo(correo)).willReturn(
				Optional.of(new Estudiante(1, "Ana", "Díaz", correo, EstadoCuenta.ACTIVA, true, Instant.now())));

		var datos = new DatosDeRegistro("ana.diaz@ucundinamarca.edu.co", "UnaClaveSegura1!", "Ana", "Díaz", true);

		assertThatThrownBy(() -> servicio.ejecutar(datos)).isInstanceOf(CorreoYaRegistradoException.class);
		verify(repositorio, never()).guardarConCredencialLocal(any(), any());
		verify(repositorio, never()).reactivarConCredencialLocal(any(), any());
		verify(emisor, never()).emitirYEnviar(any(), eq(PropositoDelCodigo.VERIFICAR_CORREO));
	}

	@Test
	void rechazaUnCorreoQueYaTieneCuentaPendiente() {
		var correo = new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co");
		given(repositorio.buscarPorCorreo(correo)).willReturn(
				Optional.of(new Estudiante(1, "Ana", "Díaz", correo, EstadoCuenta.PENDIENTE, true, Instant.now())));

		var datos = new DatosDeRegistro("ana.diaz@ucundinamarca.edu.co", "UnaClaveSegura1!", "Ana", "Díaz", true);

		assertThatThrownBy(() -> servicio.ejecutar(datos)).isInstanceOf(CorreoYaRegistradoException.class);
	}

	@Test
	void siLaCuentaConEseCorreoEstaInactivaSeRegistraDeNuevoSobreLaMisma() {
		var correo = new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co");
		given(repositorio.buscarPorCorreo(correo)).willReturn(
				Optional.of(new Estudiante(1, "Ana Vieja", "Díaz", correo, EstadoCuenta.INACTIVA, true, Instant.now())));
		given(cifrador.cifrar("UnaClaveNueva1!")).willReturn("hash-nuevo");
		given(repositorio.reactivarConCredencialLocal(any(), any())).willAnswer(inv ->
				inv.getArgument(0, Estudiante.class));

		var datos = new DatosDeRegistro("ana.diaz@ucundinamarca.edu.co", "UnaClaveNueva1!", "Ana Nueva", "Díaz", true);
		Estudiante registrado = servicio.ejecutar(datos);

		assertThat(registrado.id()).isEqualTo(1);
		assertThat(registrado.nombres()).isEqualTo("Ana Nueva");
		assertThat(registrado.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
		verify(repositorio).reactivarConCredencialLocal(any(), org.mockito.ArgumentMatchers.eq("hash-nuevo"));
		verify(repositorio, never()).guardarConCredencialLocal(any(), any());
		verify(emisor).emitirYEnviar(registrado, PropositoDelCodigo.VERIFICAR_CORREO);
	}

	@Test
	void rechazaUnCorreoQueNoEsInstitucionalAntesDeConsultarElRepositorio() {
		var datos = new DatosDeRegistro("ana.diaz@gmail.com", "UnaClaveSegura1!", "Ana", "Díaz", true);

		assertThatThrownBy(() -> servicio.ejecutar(datos)).isInstanceOf(ReglaDeNegocioVioladaException.class);
		verify(repositorio, never()).buscarPorCorreo(any());
	}
}
