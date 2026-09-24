package co.edu.ucundinamarca.cundiapp.infraestructura.configuracion;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.VerificarCorreo;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.GeneradorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.EmisorDeCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.ListarCategoriasDeRecursoServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.RegistrarEstudianteServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.ReenviarCodigoServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.VerificarCorreoServicio;
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
	ReenviarCodigoDeVerificacion reenviarCodigoDeVerificacion(
			EstudianteRepositorio estudiantes,
			CodigoDeVerificacionRepositorio codigos,
			EmisorDeCodigoDeVerificacion emisor,
			RelojPort reloj) {
		return new ReenviarCodigoServicio(estudiantes, codigos, emisor, reloj);
	}
}
