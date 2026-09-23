package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.DatosDeRegistro;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.CuentaRegistradaDto;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.RegistroDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registro e inicio de sesión (RF01): las únicas rutas públicas fuera de la guía institucional. */
@RestController
@RequestMapping("/api/publico/auth")
class CuentaController {

	private final RegistrarEstudiante registrarEstudiante;

	CuentaController(RegistrarEstudiante registrarEstudiante) {
		this.registrarEstudiante = registrarEstudiante;
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	CuentaRegistradaDto registro(@Valid @RequestBody RegistroDto datos) {
		var estudiante = registrarEstudiante.ejecutar(new DatosDeRegistro(
				datos.correo(), datos.contrasena(), datos.nombres(), datos.apellidos(), datos.aceptaTratamientoDatos()));
		return CuentaRegistradaDto.desde(estudiante);
	}
}
