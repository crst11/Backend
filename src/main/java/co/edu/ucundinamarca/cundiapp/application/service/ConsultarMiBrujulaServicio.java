package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula;
import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.RiesgoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotaRequerida;
import co.edu.ucundinamarca.cundiapp.domain.service.EvaluadorDeRiesgo;
import java.util.Comparator;
import java.util.List;

/**
 * El riesgo de cada asignatura que el estudiante cursa ahora (RF09, SCRUM-35).
 *
 * <p>Cada una se proyecta con la meta y las notas reales del estudiante y se evalúa con sus umbrales.
 * Nada se guarda: el riesgo se calcula en cada consulta, así que cambiar los umbrales o anotar una nota
 * lo recalcula sin pasos intermedios.
 */
public class ConsultarMiBrujulaServicio implements ConsultarMiBrujula {

	private final EstructuraDeEvaluacionRepositorio evaluaciones;
	private final ConfiguracionRepositorio configuracion;
	private final CalculadoraDeNotaRequerida calculadora;
	private final EvaluadorDeRiesgo evaluador;

	public ConsultarMiBrujulaServicio(
			EstructuraDeEvaluacionRepositorio evaluaciones,
			ConfiguracionRepositorio configuracion,
			CalculadoraDeNotaRequerida calculadora,
			EvaluadorDeRiesgo evaluador) {
		this.evaluaciones = evaluaciones;
		this.configuracion = configuracion;
		this.calculadora = calculadora;
		this.evaluador = evaluador;
	}

	@Override
	public Resultado ejecutar(int idEstudiante) {
		Meta meta = configuracion.metaDe(idEstudiante);
		UmbralesDeRiesgo umbrales = configuracion.umbralesDe(idEstudiante);

		List<ItemDeBrujula> items = evaluaciones.matriculasDe(idEstudiante).stream()
				.filter(AsignaturaMatriculada::estaEnCurso)
				.map(asignatura -> new ItemDeBrujula(asignatura, riesgoDe(idEstudiante, asignatura, meta, umbrales)))
				.sorted(Comparator.comparing(ItemDeBrujula::riesgo, RiesgoDeAsignatura.PRIORIDAD))
				.toList();
		return new Resultado(meta, umbrales, items);
	}

	private RiesgoDeAsignatura riesgoDe(
			int idEstudiante, AsignaturaMatriculada asignatura, Meta meta, UmbralesDeRiesgo umbrales) {
		return evaluaciones.buscar(idEstudiante, asignatura.idMatricula())
				.filter(EstructuraDeEvaluacion::estaDefinida)
				.map(estructura -> evaluador.evaluar(
						calculadora.proyectar(estructura, evaluaciones.notasDe(idEstudiante, asignatura.idMatricula()), meta),
						umbrales))
				.orElseGet(RiesgoDeAsignatura::sinDatos);
	}
}
