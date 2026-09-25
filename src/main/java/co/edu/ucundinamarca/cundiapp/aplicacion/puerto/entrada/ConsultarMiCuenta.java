package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;

/** Caso de uso: el estudiante autenticado consulta su propia cuenta. El id sale del token, nunca de la ruta. */
public interface ConsultarMiCuenta {

	Estudiante ejecutar(int idEstudiante);
}
