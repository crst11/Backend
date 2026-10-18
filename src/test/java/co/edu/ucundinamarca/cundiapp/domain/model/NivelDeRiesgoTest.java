package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Cuándo un cambio de nivel de riesgo se le avisa a la persona (RF10, SCRUM-50). */
class NivelDeRiesgoTest {

	@ParameterizedTest
	@CsvSource({"BAJO, MEDIO", "BAJO, ALTO", "MEDIO, ALTO", "ALTO, MEDIO", "ALTO, BAJO", "MEDIO, BAJO"})
	void unCambioEntreNivelesConDatosSeAvisa(NivelDeRiesgo antes, NivelDeRiesgo ahora) {
		assertThat(antes.seAvisaAlPasarA(ahora)).isTrue();
	}

	@ParameterizedTest
	@CsvSource({"BAJO, BAJO", "MEDIO, MEDIO", "ALTO, ALTO", "SIN_DATOS, SIN_DATOS"})
	void quedarseEnElMismoNivelNoEsUnCambio(NivelDeRiesgo antes, NivelDeRiesgo ahora) {
		assertThat(antes.seAvisaAlPasarA(ahora)).isFalse();
	}

	@ParameterizedTest
	@CsvSource({"SIN_DATOS, BAJO", "SIN_DATOS, ALTO", "BAJO, SIN_DATOS", "ALTO, SIN_DATOS"})
	void entrarOSalirDeSinDatosNoSeAvisa(NivelDeRiesgo antes, NivelDeRiesgo ahora) {
		assertThat(antes.seAvisaAlPasarA(ahora)).isFalse();
	}

	@Test
	void subirDeNivelEsEmpeorar() {
		assertThat(NivelDeRiesgo.BAJO.empeoraAlPasarA(NivelDeRiesgo.MEDIO)).isTrue();
		assertThat(NivelDeRiesgo.BAJO.empeoraAlPasarA(NivelDeRiesgo.ALTO)).isTrue();
		assertThat(NivelDeRiesgo.MEDIO.empeoraAlPasarA(NivelDeRiesgo.ALTO)).isTrue();
	}

	@Test
	void bajarDeNivelEsMejorar() {
		assertThat(NivelDeRiesgo.ALTO.empeoraAlPasarA(NivelDeRiesgo.MEDIO)).isFalse();
		assertThat(NivelDeRiesgo.MEDIO.empeoraAlPasarA(NivelDeRiesgo.BAJO)).isFalse();
	}
}
