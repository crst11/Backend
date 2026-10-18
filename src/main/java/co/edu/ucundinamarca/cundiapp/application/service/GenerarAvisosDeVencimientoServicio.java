package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.GenerarAvisosDeVencimiento;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio.PendientePorAvisar;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAviso;
import co.edu.ucundinamarca.cundiapp.domain.service.RedactorDeAvisos;
import java.time.Duration;
import java.time.Instant;

/**
 * Crea el aviso de cada pendiente que entró en la anticipación de su dueño (RF10, SCRUM-37) y el de cada uno
 * que se venció hace poco (SCRUM-204), sin importar su prioridad.
 */
public class GenerarAvisosDeVencimientoServicio implements GenerarAvisosDeVencimiento {

	/**
	 * Hasta cuándo después de vencido se avisa. Pasado ese tiempo el aviso ya no sirve, y así encender la función
	 * no llena la campana de pendientes viejos.
	 */
	static final Duration VIGENCIA_DEL_AVISO_DE_VENCIDO = Duration.ofHours(48);

	private final AvisoRepositorio avisos;
	private final RedactorDeAvisos redactor;
	private final RelojPort reloj;

	public GenerarAvisosDeVencimientoServicio(AvisoRepositorio avisos, RedactorDeAvisos redactor, RelojPort reloj) {
		this.avisos = avisos;
		this.redactor = redactor;
		this.reloj = reloj;
	}

	@Override
	public int ejecutar() {
		Instant ahora = reloj.ahora();
		int creados = 0;
		for (PendientePorAvisar pendiente : avisos.pendientesPorAvisar(ahora)) {
			RedactorDeAvisos.Texto texto = redactor.vencimiento(pendiente.titulo(), pendiente.fechaLimite(), ahora);
			Aviso aviso = Aviso.nuevo(pendiente.idEstudiante(), TipoDeAviso.VENCIMIENTO_PROXIMO, texto.titulo(), texto.mensaje(), ahora);
			// Falso si otra ronda u otra instancia ya lo creó: la base de datos no admite dos avisos por pendiente.
			if (avisos.crearDeVencimiento(aviso, pendiente.idPendiente())) {
				creados++;
			}
		}
		for (PendientePorAvisar vencido : avisos.pendientesVencidos(ahora, VIGENCIA_DEL_AVISO_DE_VENCIDO)) {
			RedactorDeAvisos.Texto texto = redactor.vencido(vencido.titulo(), vencido.fechaLimite(), ahora);
			Aviso aviso = Aviso.nuevo(vencido.idEstudiante(), TipoDeAviso.PENDIENTE_VENCIDO, texto.titulo(), texto.mensaje(), ahora);
			if (avisos.crearDeVencido(aviso, vencido.idPendiente())) {
				creados++;
			}
		}
		return creados;
	}
}
