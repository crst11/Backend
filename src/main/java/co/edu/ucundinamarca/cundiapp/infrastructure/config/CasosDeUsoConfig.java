package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import co.edu.ucundinamarca.cundiapp.application.port.in.CerrarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiCuenta;
import co.edu.ucundinamarca.cundiapp.application.port.in.IniciarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.application.port.in.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RenovarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.VerificarCorreo;
import co.edu.ucundinamarca.cundiapp.application.port.out.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.GeneradorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDeIntentosPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.service.CerrarSesionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiCuentaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.EmisorDeCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.application.service.IniciarSesionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ListarCategoriasDeRecursoServicio;
import co.edu.ucundinamarca.cundiapp.application.service.RegistrarEstudianteServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ReenviarCodigoServicio;
import co.edu.ucundinamarca.cundiapp.application.service.RenovarSesionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.VerificarCorreoServicio;
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
