package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class CatalogoAcademicoAdaptador implements ProgramaAcademicoRepositorio {

	private static final String VIGENTE = "vigente";

	private final ProgramaAcademicoJpa programas;
	private final PlanDeEstudiosJpa planes;
	private final AsignaturaJpa asignaturas;
	private final PrerrequisitoJpa prerrequisitos;

	CatalogoAcademicoAdaptador(
			ProgramaAcademicoJpa programas,
			PlanDeEstudiosJpa planes,
			AsignaturaJpa asignaturas,
			PrerrequisitoJpa prerrequisitos) {
		this.programas = programas;
		this.planes = planes;
		this.asignaturas = asignaturas;
		this.prerrequisitos = prerrequisitos;
	}

	@Override
	public List<ProgramaAcademico> listarProgramas() {
		return programas.conPlanVigente().stream().map(ProgramaAcademicoEntidad::aDominio).toList();
	}

	@Override
	public Optional<PlanDeEstudios> buscarPlanVigenteDe(String codigoPrograma) {
		return planes.findByCodigoProgramaAndEstado(codigoPrograma, VIGENTE).map(this::armar);
	}

	@Override
	public Optional<PlanDeEstudios> buscarPlanPorCodigo(String codigoPlan) {
		return planes.findById(codigoPlan).map(this::armar);
	}

	/**
	 * Arma el plan con sus asignaturas y requisitos. Los requisitos se traen de una vez y se agrupan en
	 * memoria, en vez de una consulta por asignatura: son 60 asignaturas y 64 requisitos.
	 */
	private PlanDeEstudios armar(PlanDeEstudiosEntidad plan) {
		Map<String, List<String>> requisitosPorAsignatura = prerrequisitos.delPlan(plan.codigo()).stream()
				.collect(Collectors.groupingBy(
						PrerrequisitoEntidad::codigoAsignatura,
						Collectors.mapping(PrerrequisitoEntidad::codigoRequerida, Collectors.toList())));

		List<co.edu.ucundinamarca.cundiapp.domain.model.Asignatura> delPlan =
				asignaturas.findByCodigoPlan(plan.codigo()).stream()
						.map(entidad -> entidad.aDominio(
								requisitosPorAsignatura.getOrDefault(entidad.codigo(), List.of())))
						.toList();

		ProgramaAcademico programa = programas.findById(plan.codigoPrograma()).orElseThrow()
				.aDominio();
		return new PlanDeEstudios(plan.codigo(), programa, delPlan);
	}
}
