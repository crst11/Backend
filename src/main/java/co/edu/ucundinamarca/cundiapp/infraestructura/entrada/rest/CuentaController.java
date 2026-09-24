package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.DatosDeRegistro;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.VerificarCorreo;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.CuentaRegistradaDto;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.RegistroDto;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.ReenvioDto;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.VerificacionDto;
import jakarta.validation.Valid;
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

	private final RegistrarEstudiante registrarEstudiante;
	private final VerificarCorreo verificarCorreo;
	private final ReenviarCodigoDeVerificacion reenviarCodigo;

	CuentaController(
			RegistrarEstudiante registrarEstudiante,
			VerificarCorreo verificarCorreo,
			ReenviarCodigoDeVerificacion reenviarCodigo) {
		this.registrarEstudiante = registrarEstudiante;
		this.verificarCorreo = verificarCorreo;
		this.reenviarCodigo = reenviarCodigo;
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	CuentaRegistradaDto registro(@Valid @RequestBody RegistroDto datos) {
		var estudiante = registrarEstudiante.ejecutar(new DatosDeRegistro(
				datos.correo(), datos.contrasena(), datos.nombres(), datos.apellidos(), datos.aceptaTratamientoDatos()));
		return CuentaRegistradaDto.desde(estudiante);
	}

	@PostMapping("/verificacion")
	CuentaRegistradaDto verificar(@Valid @RequestBody VerificacionDto datos) {
		return CuentaRegistradaDto.desde(verificarCorreo.ejecutar(datos.correo(), datos.codigo()));
	}

	@PostMapping("/verificacion/reenvio")
	@ResponseStatus(HttpStatus.ACCEPTED)
	void reenviar(@Valid @RequestBody ReenvioDto datos) {
		reenviarCodigo.ejecutar(datos.correo());
	}
}
