package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;

/** Caso de uso: crear la cuenta de un estudiante con correo institucional (RF01). */
public interface RegistrarEstudiante {

	Estudiante ejecutar(DatosDeRegistro datos);
}
