package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: elegir mi programa y sede, o cambiarlos después (RF02). */
public interface ElegirMiPrograma {

	/** Deja el plan vigente de ese programa como el del estudiante y devuelve el perfil resultante. */
	MiPerfilAcademico ejecutar(int idEstudiante, String codigoPrograma);
}
