package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.GenerarAvisosDeVencimiento;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio.PendientePorAvisar;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAviso;
import co.edu.ucundinamarca.cundiapp.domain.service.RedactorDeAvisos;
import java.time.Instant;

/** Crea el aviso de cada pendiente que entró en la anticipación de su dueño (RF10, SCRUM-37). */
public class GenerarAvisosDeVencimientoServicio implements GenerarAvisosDeVencimiento {

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
		return creados;
	}
}
