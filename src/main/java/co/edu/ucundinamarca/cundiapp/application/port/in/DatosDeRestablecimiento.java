package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Modelo de entrada del caso de uso, independiente del DTO que use el adaptador REST. */
public record DatosDeRestablecimiento(String correo, String codigo, String contrasenaNuevaSinCifrar) {
}
