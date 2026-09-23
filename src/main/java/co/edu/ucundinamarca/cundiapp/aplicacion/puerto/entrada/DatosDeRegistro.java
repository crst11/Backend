package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada;

/** Modelo de entrada del caso de uso, independiente del DTO que use el adaptador REST. */
public record DatosDeRegistro(
		String correo,
		String contrasenaSinCifrar,
		String nombres,
		String apellidos,
		boolean aceptaTratamientoDatos) {
}
