package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Desde dónde se abre la sesión: sirve para el límite de intentos y para listar dispositivos. */
public record OrigenDeSesion(String ip, String userAgent) {
}
