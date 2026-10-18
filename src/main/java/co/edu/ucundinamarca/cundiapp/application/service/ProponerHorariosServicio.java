package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ProponerHorarios;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaOfertada;
import co.edu.ucundinamarca.cundiapp.domain.model.CriterioDeAcomodo;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeAcomodo;
import co.edu.ucundinamarca.cundiapp.domain.service.ArmadorDeHorario;
import java.util.List;

/** Le pasa la oferta al motor de acomodo (RF13, SCRUM-208). Las reglas están en el dominio. */
public class ProponerHorariosServicio implements ProponerHorarios {

	private final ArmadorDeHorario armador;

	public ProponerHorariosServicio(ArmadorDeHorario armador) {
		this.armador = armador;
	}

	@Override
	public ResultadoDeAcomodo ejecutar(List<AsignaturaOfertada> oferta, CriterioDeAcomodo criterio) {
		return armador.proponer(oferta, criterio);
	}
}
