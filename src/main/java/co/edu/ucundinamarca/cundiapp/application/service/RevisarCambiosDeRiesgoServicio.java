package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula.ItemDeBrujula;
import co.edu.ucundinamarca.cundiapp.application.port.in.RevisarCambiosDeRiesgo;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciasDeAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RiesgoAvisadoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAviso;
import co.edu.ucundinamarca.cundiapp.domain.service.RedactorDeAvisos;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Avisa cuando el riesgo de una asignatura cambia de nivel (RF10, SCRUM-50).
 *
 * <p>El nivel de ahora lo da la Brújula (RF09), que lo calcula con la meta y los umbrales del estudiante; aquí
 * solo se compara con el último nivel conocido. Tres reglas:
 * <ul>
 *   <li>La primera vez que se calcula el riesgo de una asignatura solo se guarda el nivel: sin un «antes» no
 *       hay cambio que contar.</li>
 *   <li>Entrar o salir de «sin datos» no se avisa: no es que empeore ni mejore, es que ahora se puede calcular.</li>
 *   <li>Si la persona apagó los avisos de cambio de riesgo (o todos), el nivel igual se actualiza, para que al
 *       volver a encenderlos no le lleguen cambios viejos.</li>
 * </ul>
 */
public class RevisarCambiosDeRiesgoServicio implements RevisarCambiosDeRiesgo {

	private final ConsultarMiBrujula brujula;
	private final PreferenciasDeAvisosRepositorio preferencias;
	private final RiesgoAvisadoRepositorio riesgoAvisado;
	private final AvisoRepositorio avisos;
	private final RedactorDeAvisos redactor;
	private final RelojPort reloj;

	public RevisarCambiosDeRiesgoServicio(
			ConsultarMiBrujula brujula,
			PreferenciasDeAvisosRepositorio preferencias,
			RiesgoAvisadoRepositorio riesgoAvisado,
			AvisoRepositorio avisos,
			RedactorDeAvisos redactor,
			RelojPort reloj) {
		this.brujula = brujula;
		this.preferencias = preferencias;
		this.riesgoAvisado = riesgoAvisado;
		this.avisos = avisos;
		this.redactor = redactor;
		this.reloj = reloj;
	}

	@Override
	public void deLaAsignatura(int idEstudiante, int idMatricula) {
		revisar(idEstudiante, asignaturasDe(idEstudiante).stream()
				.filter(item -> item.asignatura().idMatricula() == idMatricula)
				.toList());
	}

	@Override
	public void deTodasLasAsignaturas(int idEstudiante) {
		revisar(idEstudiante, asignaturasDe(idEstudiante));
	}

	private List<ItemDeBrujula> asignaturasDe(int idEstudiante) {
		return brujula.ejecutar(idEstudiante).asignaturas();
	}

	private void revisar(int idEstudiante, List<ItemDeBrujula> items) {
		if (items.isEmpty()) {
			return;
		}
		Instant ahora = reloj.ahora();
		PreferenciasDeAvisos quiere = preferencias.de(idEstudiante);
		for (ItemDeBrujula item : items) {
			int idMatricula = item.asignatura().idMatricula();
			NivelDeRiesgo nivel = item.riesgo().nivel();
			Optional<NivelDeRiesgo> antes = riesgoAvisado.nivelDe(idMatricula);
			if (antes.filter(nivel::equals).isPresent()) {
				continue;
			}
			if (antes.isPresent() && antes.get().seAvisaAlPasarA(nivel) && quiere.activos() && quiere.cambioDeRiesgo()) {
				RedactorDeAvisos.Texto texto = redactor.cambioDeRiesgo(item.asignatura().nombre(), antes.get(), nivel);
				avisos.crearDeCambioDeRiesgo(
						Aviso.nuevo(idEstudiante, TipoDeAviso.CAMBIO_RIESGO, texto.titulo(), texto.mensaje(), ahora), idMatricula);
			}
			riesgoAvisado.guardar(idMatricula, nivel, ahora);
		}
	}
}
