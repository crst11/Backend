package co.edu.ucundinamarca.cundiapp.infraestructura.configuracion;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.servicio.ListarCategoriasDeRecursoServicio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Aquí Spring conoce la aplicación, no al revés: ensambla los casos de uso con sus puertos de salida. */
@Configuration
class CasosDeUsoConfig {

	@Bean
	ListarCategoriasDeRecurso listarCategoriasDeRecurso(CategoriaDeRecursoRepositorio repositorio) {
		return new ListarCategoriasDeRecursoServicio(repositorio);
	}
}
