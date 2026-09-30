package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: entrar con un toque usando la cuenta de Google que el estudiante ya vinculó (SCRUM-48). */
public interface IniciarSesionConGoogle {

	SesionIniciada ejecutar(String idToken, OrigenDeSesion origen);
}
