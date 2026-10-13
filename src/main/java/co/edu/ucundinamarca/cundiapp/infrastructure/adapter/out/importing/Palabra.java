package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

/**
 * Una palabra de un PDF y dónde está (SCRUM-81): de qué x a qué x, y a qué distancia del borde
 * superior. `arriba` ya incluye el desplazamiento de la página, así que las palabras de varias
 * páginas se leen como una sola columna larga.
 */
record Palabra(String texto, double x0, double x1, double arriba) {

	double centro() {
		return (x0 + x1) / 2;
	}
}
