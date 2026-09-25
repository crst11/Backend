package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Contra PostgreSQL real: guarda la huella, cuenta intentos y reemplaza el código al emitir otro. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CodigoDeVerificacionAdaptadorTest {

	@Autowired
	private CodigoDeVerificacionRepositorio codigos;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Test
	void guardaCuentaIntentosYReemplazaElCodigoAnterior() {
		var estudiante = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Prueba", "Código",
						new CorreoInstitucional("codigo.test@ucundinamarca.edu.co"),
						EstadoCuenta.PENDIENTE, true, Instant.now()),
				"hash-de-prueba");
		Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		assertThat(codigos.buscarDe(estudiante.id())).isEmpty();

		var primero = CodigoDeVerificacion.emitir(estudiante.id(), "111111", ahora);
		codigos.guardar(primero);
		var tras = primero.intentar("000000", ahora).codigo();
		codigos.guardar(tras);

		assertThat(codigos.buscarDe(estudiante.id()).orElseThrow().intentosFallidos()).isEqualTo(1);

		var segundo = CodigoDeVerificacion.emitir(estudiante.id(), "222222", ahora);
		codigos.guardar(segundo);

		var guardado = codigos.buscarDe(estudiante.id()).orElseThrow();
		assertThat(guardado.intentosFallidos()).isZero();
		assertThat(guardado.huella()).isEqualTo(segundo.huella());
	}
}
