package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.component.LimiteDeAnalisisDePdf;
import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarReporteDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDePeticionesPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteDeHorarioPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionDeHorarioRepositorio.MatriculaConClases;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.ReporteDeHorarioDetectado;
import java.text.Normalizer;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lee el Reporte de Horario y lo cruza con la ruta de aprendizaje del estudiante para mostrarle qué
 * va a pasar si confirma (RF04, SCRUM-81). No guarda nada.
 */
public class AnalizarReporteDeHorarioServicio implements AnalizarReporteDeHorario {

	private final FuenteDeHorarioPort fuente;
	private final EstudianteRepositorio estudiantes;
	private final LimitadorDePeticionesPort limitador;
	private final ProgramaAcademicoRepositorio programas;
	private final ImportacionDeHorarioRepositorio horarios;

	public AnalizarReporteDeHorarioServicio(
			FuenteDeHorarioPort fuente,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas,
			ImportacionDeHorarioRepositorio horarios,
			LimitadorDePeticionesPort limitador) {
		this.limitador = limitador;
		this.fuente = fuente;
		this.estudiantes = estudiantes;
		this.programas = programas;
		this.horarios = horarios;
	}

	@Override
	public Analisis ejecutar(int idEstudiante, byte[] archivo) {
		LimiteDeAnalisisDePdf.exigirCupo(limitador, idEstudiante);
		PlanDeEstudios plan = estudiantes.planDe(idEstudiante)
				.flatMap(programas::buscarPlanPorCodigo)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"Primero elige tu programa: sin la ruta de aprendizaje no sabemos a qué asignaturas corresponde tu horario"));

		ReporteDeHorarioDetectado reporte = fuente.leer(archivo);
		Map<String, Asignatura> delPlan = plan.asignaturas().stream()
				.collect(Collectors.toMap(Asignatura::codigo, Function.identity()));
		Map<String, MatriculaConClases> yaTiene = horarios.matriculasDe(idEstudiante, reporte.periodo()).stream()
				.collect(Collectors.toMap(MatriculaConClases::codigoAsignatura, Function.identity()));

		var asignaturas = reporte.asignaturas().stream()
				.map(detectada -> {
					Asignatura delPrograma = delPlan.get(detectada.codigo());
					MatriculaConClases matricula = yaTiene.get(detectada.codigo());
					return new AsignaturaPorImportar(
							detectada.codigo(),
							delPrograma == null ? detectada.codigo() : delPrograma.nombre(),
							detectada.grupo(),
							delPrograma != null,
							matricula != null,
							matricula == null ? 0 : matricula.clases(),
							detectada.bloques());
				})
				.toList();

		return new Analisis(
				reporte.programa(),
				reporte.sede(),
				normalizar(plan.programa().nombre()).equals(normalizar(reporte.programa())),
				reporte.periodo().codigo(),
				asignaturas);
	}

	private static String normalizar(String texto) {
		return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase()
				.replaceAll("\\s+", " ")
				.trim();
	}
}
