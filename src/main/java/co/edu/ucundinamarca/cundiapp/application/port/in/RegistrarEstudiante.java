package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;

/** Caso de uso: crear la cuenta de un estudiante con correo institucional (RF01). */
public interface RegistrarEstudiante {

	Estudiante ejecutar(DatosDeRegistro datos);
}
