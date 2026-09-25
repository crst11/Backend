package co.edu.ucundinamarca.cundiapp.infraestructura.configuracion;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.CerrarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ConsultarMiCuenta;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.IniciarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RenovarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.VerificarCorreo;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.GeneradorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.LimitadorDeIntentosPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.CerrarSesionServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.ConsultarMiCuentaServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.EmisorDeCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.IniciarSesionServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.ListarCategoriasDeRecursoServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.RegistrarEstudianteServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.ReenviarCodigoServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.RenovarSesionServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.VerificarCorreoServicio;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Aquí Spring conoce la aplicación, no al revés: ensambla los casos de uso con sus puertos de salida. */
@Configuration
class CasosDeUsoConfig {

	@Bean
	ListarCategoriasDeRecurso listarCategoriasDeRecurso(CategoriaDeRecursoRepositorio repositorio) {
		return new ListarCategoriasDeRecursoServicio(repositorio);
	}

	@Bean
	EmisorDeCodigoDeVerificacion emisorDeCodigoDeVerificacion(
			GeneradorDeCodigoPort generador,
			EnviadorDeCodigoPort enviador,
			CodigoDeVerificacionRepositorio codigos,
			RelojPort reloj) {
		return new EmisorDeCodigoDeVerificacion(generador, enviador, codigos, reloj);
	}

	@Bean
	RegistrarEstudiante registrarEstudiante(
			EstudianteRepositorio repositorio,
			CifradorDeContrasenaPort cifrador,
			RelojPort reloj,
			EmisorDeCodigoDeVerificacion emisor) {
		return new RegistrarEstudianteServicio(repositorio, cifrador, reloj, emisor);
	}

	@Bean
	VerificarCorreo verificarCorreo(
			EstudianteRepositorio estudiantes, CodigoDeVerificacionRepositorio codigos, RelojPort reloj) {
		return new VerificarCorreoServicio(estudiantes, codigos, reloj);
	}

	@Bean
	IniciarSesion iniciarSesion(
			EstudianteRepositorio estudiantes,
			SesionRepositorio sesiones,
			CifradorDeContrasenaPort cifrador,
			EmisorDeTokensPort tokens,
			LimitadorDeIntentosPort limitador,
			RelojPort reloj,
			@Value("${cundiapp.jwt.duracion-acceso}") Duration vigenciaAcceso,
			@Value("${cundiapp.jwt.duracion-refresco}") Duration vigenciaRefresco) {
		return new IniciarSesionServicio(
				estudiantes, sesiones, cifrador, tokens, limitador, reloj, vigenciaAcceso, vigenciaRefresco);
	}

	@Bean
	RenovarSesion renovarSesion(
			EstudianteRepositorio estudiantes,
			SesionRepositorio sesiones,
			EmisorDeTokensPort tokens,
			RelojPort reloj,
			@Value("${cundiapp.jwt.duracion-acceso}") Duration vigenciaAcceso,
			@Value("${cundiapp.jwt.duracion-refresco}") Duration vigenciaRefresco) {
		return new RenovarSesionServicio(estudiantes, sesiones, tokens, reloj, vigenciaAcceso, vigenciaRefresco);
	}

	@Bean
	CerrarSesion cerrarSesion(SesionRepositorio sesiones, RelojPort reloj) {
		return new CerrarSesionServicio(sesiones, reloj);
	}

	@Bean
	ConsultarMiCuenta consultarMiCuenta(EstudianteRepositorio estudiantes) {
		return new ConsultarMiCuentaServicio(estudiantes);
	}

	@Bean
	ReenviarCodigoDeVerificacion reenviarCodigoDeVerificacion(
			EstudianteRepositorio estudiantes,
			CodigoDeVerificacionRepositorio codigos,
			EmisorDeCodigoDeVerificacion emisor,
			RelojPort reloj) {
		return new ReenviarCodigoServicio(estudiantes, codigos, emisor, reloj);
	}
}
