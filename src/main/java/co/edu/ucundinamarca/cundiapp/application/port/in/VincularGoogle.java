package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;

/** Caso de uso: el estudiante, con su correo institucional verificado, vincula su cuenta de Google. */
public interface VincularGoogle {

	VinculoConGoogle ejecutar(int idEstudiante, String idToken);
}
