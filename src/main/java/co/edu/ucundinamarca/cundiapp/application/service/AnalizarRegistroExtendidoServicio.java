package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarRegistroExtendido;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteHistorialAcademicoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.HistorialAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaPorImportar;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoCursado;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoPorImportar;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.ReporteAcademicoDetectado;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lee el reporte y lo cruza con la ruta de aprendizaje del estudiante para mostrarle qué va a
 * pasar si confirma (RF03, SCRUM-23). No guarda nada.
 */
public class AnalizarRegistroExtendidoServicio implements AnalizarRegistroExtendido {

	private final FuenteHistorialAcademicoPort fuente;
	private final EstudianteRepositorio estudiantes;
	private final ProgramaAcademicoRepositorio programas;
	private final HistorialAcademicoRepositorio historiales;

	public AnalizarRegistroExtendidoServicio(
			FuenteHistorialAcademicoPort fuente,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas,
			HistorialAcademicoRepositorio historiales) {
		this.fuente = fuente;
		this.estudiantes = estudiantes;
		this.programas = programas;
		this.historiales = historiales;
	}

	@Override
	public Analisis ejecutar(int idEstudiante, byte[] archivo) {
		Optional<PlanDeEstudios> plan = estudiantes.planDe(idEstudiante)
				.flatMap(programas::buscarPlanPorCodigo);
		if (plan.isEmpty()) {
			throw new ReglaDeNegocioVioladaException(
					"Primero elige tu programa: sin la ruta de aprendizaje no sabemos a qué asignaturas corresponden tus notas");
		}

		ReporteAcademicoDetectado reporte = fuente.leer(archivo);
		Map<String, Asignatura> delPlan = plan.get().asignaturas().stream()
				.collect(Collectors.toMap(Asignatura::codigo, Function.identity()));
		Set<String> yaCursadas = yaCursadasDe(idEstudiante);

		List<PeriodoPorImportar> periodos = reporte.periodos().stream()
				.map(periodo -> new PeriodoPorImportar(
						periodo.codigo(),
						periodo.asignaturas().stream()
								.map(detectada -> {
									Asignatura asignatura = delPlan.get(detectada.codigo());
									return new AsignaturaPorImportar(
											detectada.codigo(),
											asignatura == null ? detectada.codigo() : asignatura.nombre(),
											asignatura == null ? 0 : asignatura.creditos(),
											detectada.notaDefinitiva(),
											detectada.estado(),
											asignatura != null,
											yaCursadas.contains(claveDe(periodo.codigo(), detectada.codigo())));
								})
								.toList(),
						reporte.oficiales().stream()
								.filter(oficial -> oficial.codigoPeriodo().equals(periodo.codigo()))
								.findFirst()
								.orElse(null)))
				.toList();

		return new Analisis(
				reporte.programa(),
				reporte.sede(),
				mismoPrograma(plan.get(), reporte),
				periodos);
	}

	/** Lo que el estudiante ya tiene cargado, para poder decirle qué es nuevo y qué se actualiza. */
	private Set<String> yaCursadasDe(int idEstudiante) {
		Set<String> claves = new HashSet<>();
		for (PeriodoCursado periodo : historiales.historialDe(idEstudiante).periodos()) {
			periodo.asignaturas().forEach(
					asignatura -> claves.add(claveDe(periodo.codigo(), asignatura.codigo())));
		}
		return claves;
	}

	private static String claveDe(String periodo, String asignatura) {
		return periodo + "|" + asignatura;
	}

	/**
	 * Compara por el nombre del programa, que es lo único que trae el reporte. Si no coincide no se
	 * bloquea nada: se le avisa al estudiante, porque puede haber cambiado de programa o haber
	 * subido el reporte equivocado, y quien sabe cuál es el caso es él.
	 */
	private static boolean mismoPrograma(PlanDeEstudios plan, ReporteAcademicoDetectado reporte) {
		return normalizar(plan.programa().nombre()).equals(normalizar(reporte.programa()));
	}

	private static String normalizar(String texto) {
		return java.text.Normalizer.normalize(texto == null ? "" : texto, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase()
				.replaceAll("\\s+", " ")
				.trim();
	}
}
