package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.List;

/**
 * Una combinación sin choques: un grupo por asignatura, con su puntaje de 0 a 100, sus números y el motivo en
 * palabras (RF13, SCRUM-207). Lo calcula el dominio; la pantalla solo lo muestra.
 *
 * @param puntaje qué tanto cumple lo que el estudiante pidió; 100 si no pidió nada
 * @param motivos frases que explican por qué se propone esta combinación
 */
public record PropuestaDeHorario(int puntaje, List<Eleccion> elecciones, Resumen resumen, List<String> motivos) {

	public PropuestaDeHorario {
		elecciones = List.copyOf(elecciones);
		motivos = List.copyOf(motivos);
	}

	/** El grupo elegido de una asignatura, con sus bloques para que la pantalla pueda dibujarlos. */
	public record Eleccion(String asignatura, String grupo, List<BloqueDeOferta> bloques) {

		public Eleccion {
			bloques = List.copyOf(bloques);
		}
	}

	/**
	 * Los números de la combinación. {@code diasLibres} son los de lunes a sábado sin clase.
	 */
	public record Resumen(
			int minutosEnLaManana,
			int minutosEnLaTarde,
			int minutosEnLaNoche,
			int minutosDeHuecos,
			List<DiaDeLaSemana> diasConClase,
			List<DiaDeLaSemana> diasLibres) {

		public Resumen {
			diasConClase = List.copyOf(diasConClase);
			diasLibres = List.copyOf(diasLibres);
		}

		public int minutosDeClase() {
			return minutosEnLaManana + minutosEnLaTarde + minutosEnLaNoche;
		}

		public int minutosEn(FranjaDelDia franja) {
			return switch (franja) {
				case MANANA -> minutosEnLaManana;
				case TARDE -> minutosEnLaTarde;
				case NOCHE -> minutosEnLaNoche;
			};
		}
	}
}
