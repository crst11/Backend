package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.DatosDeRegistro;
import co.edu.ucundinamarca.cundiapp.application.port.in.DatosDeRestablecimiento;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.application.port.in.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RestablecerContrasena;
import co.edu.ucundinamarca.cundiapp.application.port.in.SolicitarRecuperacionDeContrasena;
import co.edu.ucundinamarca.cundiapp.application.port.in.VerificarCorreo;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CuentaRegistradaDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.RegistroDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.RecuperacionDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ReenvioDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.RestablecimientoDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.VerificacionDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registro y verificación del correo (RF01): rutas públicas, todavía sin sesión. */
@RestController
@RequestMapping("/api/publico/auth")
class CuentaController {

	private static final Logger log = LoggerFactory.getLogger(CuentaController.class);

	private final RegistrarEstudiante registrarEstudiante;
	private final VerificarCorreo verificarCorreo;
	private final ReenviarCodigoDeVerificacion reenviarCodigo;
	private final SolicitarRecuperacionDeContrasena solicitarRecuperacion;
	private final RestablecerContrasena restablecerContrasena;

	CuentaController(
			RegistrarEstudiante registrarEstudiante,
			VerificarCorreo verificarCorreo,
			ReenviarCodigoDeVerificacion reenviarCodigo,
			SolicitarRecuperacionDeContrasena solicitarRecuperacion,
			RestablecerContrasena restablecerContrasena) {
		this.registrarEstudiante = registrarEstudiante;
		this.verificarCorreo = verificarCorreo;
		this.reenviarCodigo = reenviarCodigo;
		this.solicitarRecuperacion = solicitarRecuperacion;
		this.restablecerContrasena = restablecerContrasena;
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	CuentaRegistradaDto registro(@Valid @RequestBody RegistroDto datos) {
		var estudiante = registrarEstudiante.ejecutar(new DatosDeRegistro(
				datos.correo(), datos.contrasena(), datos.nombres(), datos.apellidos(), datos.aceptaTratamientoDatos()));
		log.info("Cuenta registrada: estudiante {} pendiente de verificar el correo", estudiante.id());
		return CuentaRegistradaDto.desde(estudiante);
	}

	@PostMapping("/verificacion")
	CuentaRegistradaDto verificar(@Valid @RequestBody VerificacionDto datos) {
		var estudiante = verificarCorreo.ejecutar(datos.correo(), datos.codigo());
		log.info("Correo verificado: la cuenta del estudiante {} quedó activa", estudiante.id());
		return CuentaRegistradaDto.desde(estudiante);
	}

	@PostMapping("/verificacion/reenvio")
	@ResponseStatus(HttpStatus.ACCEPTED)
	void reenviar(@Valid @RequestBody ReenvioDto datos) {
		reenviarCodigo.ejecutar(datos.correo());
	}

	/** Responde 202 siempre, exista o no la cuenta: no se revela quién está registrado (SCRUM-68). */
	@PostMapping("/recuperacion")
	@ResponseStatus(HttpStatus.ACCEPTED)
	void solicitarRecuperacion(@Valid @RequestBody RecuperacionDto datos) {
		solicitarRecuperacion.ejecutar(datos.correo());
	}

	@PostMapping("/recuperacion/confirmacion")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void restablecer(@Valid @RequestBody RestablecimientoDto datos) {
		restablecerContrasena.ejecutar(
				new DatosDeRestablecimiento(datos.correo(), datos.codigo(), datos.contrasenaNueva()));
		// Sin el correo: el log no debe decir quién cambió su contraseña.
		log.info("Contraseña restablecida y sesiones revocadas");
	}
}
