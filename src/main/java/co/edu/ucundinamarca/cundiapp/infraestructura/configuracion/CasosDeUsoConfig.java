package co.edu.ucundinamarca.cundiapp.infraestructura.configuracion;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.ListarCategoriasDeRecursoServicio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.RegistrarEstudianteServicio;
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
	RegistrarEstudiante registrarEstudiante(
			EstudianteRepositorio repositorio, CifradorDeContrasenaPort cifrador, RelojPort reloj) {
		return new RegistrarEstudianteServicio(repositorio, cifrador, reloj);
	}
}
