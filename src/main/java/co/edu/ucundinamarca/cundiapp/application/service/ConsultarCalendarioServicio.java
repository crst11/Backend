package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarCalendario;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CalendarioAcademico;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** El calendario de un rango de fechas, con «hoy» tomado del reloj de Bogotá (SCRUM-85). */
public class ConsultarCalendarioServicio implements ConsultarCalendario {

	/** Un rango más largo que un trimestre no lo necesita ninguna pantalla y arma respuestas enormes. */
	static final int DIAS_MAXIMOS = 92;
	private static final int DIAS_POR_OMISION = 7;

	private final Clock reloj;
	private final CalendarioAcademico calendario = new CalendarioAcademico();

	public ConsultarCalendarioServicio(Clock reloj) {
		this.reloj = reloj;
	}

	@Override
	public Calendario ejecutar(LocalDate desde, LocalDate hasta) {
		LocalDate hoy = LocalDate.now(reloj);
		LocalDate inicio = desde == null ? hoy : desde;
		LocalDate fin = hasta == null ? inicio.plusDays(DIAS_POR_OMISION - 1) : hasta;
		if (fin.isBefore(inicio)) {
			throw new ReglaDeNegocioVioladaException("La fecha «desde» no puede ser posterior a «hasta»");
		}
		if (ChronoUnit.DAYS.between(inicio, fin) + 1 > DIAS_MAXIMOS) {
			throw new ReglaDeNegocioVioladaException(
					"El rango no puede pasar de %d días".formatted(DIAS_MAXIMOS));
		}
		return new Calendario(hoy, calendario.entre(inicio, fin));
	}
}
