package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.HistorialAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaCursada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.HistorialAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoCursado;
import co.edu.ucundinamarca.cundiapp.domain.model.ResumenOficialDePeriodo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Arma el historial desde `matricula_asignatura`, con el nombre y los créditos que vienen de
 * `asignatura` (SCRUM-22).
 *
 * <p>Los créditos no se guardan en la matrícula: son los de la asignatura en el plan. Copiarlos
 * sería duplicar un dato que ya existe y arriesgarse a que queden desactualizados.
 */
@Component
class HistorialAcademicoAdaptador implements HistorialAcademicoRepositorio {

	private final MatriculaAsignaturaJpa matriculas;
	private final ResumenPeriodoJpa resumenes;
	private final AsignaturaJpa asignaturas;

	HistorialAcademicoAdaptador(
			MatriculaAsignaturaJpa matriculas, ResumenPeriodoJpa resumenes, AsignaturaJpa asignaturas) {
		this.matriculas = matriculas;
		this.resumenes = resumenes;
		this.asignaturas = asignaturas;
	}

	@Override
	public HistorialAcademico historialDe(int idEstudiante) {
		List<MatriculaAsignaturaEntidad> cursadas =
				matriculas.findByIdEstudianteOrderByCodigoPeriodoAscCodigoAsignaturaAsc(idEstudiante);
		if (cursadas.isEmpty()) {
			return new HistorialAcademico(List.of());
		}

		Map<String, AsignaturaEntidad> catalogo = asignaturas
				.findAllById(cursadas.stream().map(MatriculaAsignaturaEntidad::codigoAsignatura).distinct().toList())
				.stream()
				.collect(Collectors.toMap(AsignaturaEntidad::codigo, asignatura -> asignatura));

		Map<String, List<AsignaturaCursada>> porPeriodo = new LinkedHashMap<>();
		for (MatriculaAsignaturaEntidad matricula : cursadas) {
			AsignaturaEntidad asignatura = catalogo.get(matricula.codigoAsignatura());
			porPeriodo.computeIfAbsent(matricula.codigoPeriodo(), periodo -> new java.util.ArrayList<>())
					.add(new AsignaturaCursada(
							matricula.codigoAsignatura(),
							asignatura == null ? matricula.codigoAsignatura() : asignatura.nombre(),
							asignatura == null ? 0 : asignatura.creditos(),
							matricula.notaDefinitiva(),
							EstadoDeAsignatura.desdeBd(matricula.estado())));
		}

		return new HistorialAcademico(porPeriodo.entrySet().stream()
				.map(entrada -> new PeriodoCursado(entrada.getKey(), entrada.getValue()))
				.toList());
	}

	@Override
	public List<ResumenOficialDePeriodo> resumenesOficialesDe(int idEstudiante) {
		return resumenes.de(idEstudiante).stream().map(ResumenPeriodoEntidad::aDominio).toList();
	}
}
