package co.edu.ucundinamarca.cundiapp;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SCRUM-41: las 14 restricciones de integridad que no se derivan de las llaves. Cada caso prueba
 * lo que debe rechazarse (y por cuál regla) y lo que debe aceptarse.
 */
class RestriccionesDeIntegridadTest extends PruebaConBaseDeDatos {

	private static final String HASH = "$2a$12$" + "z".repeat(53);
	private static final String HUELLA_A = "a".repeat(64);
	private static final String HUELLA_B = "b".repeat(64);

	private int ana;
	private int luis;
	private int matriculaDeAna;

	@BeforeEach
	void datosBase() {
		catalogoBasico();
		ana = cuenta("ana@ucundinamarca.edu.co");
		luis = cuenta("luis@ucundinamarca.edu.co");
		matriculaDeAna = matricula(ana, "A1");
		estructuraDeEvaluacion(matriculaDeAna);
		// No se llama a confirmar() aquí: SET CONSTRAINTS ALL IMMEDIATE dura toda la transacción y
		// volvería inmediatas las sumas de 100 %, que cada prueba debe poder armar por partes.
	}

	@Nested
	class Restriccion1_CategoriasSuman100 {
		@Test
		void rechazaCategoriasQueSuman90() {
			int otra = matricula(ana, "A2");
			categoria(otra, 1, 50);
			categoria(otra, 2, 40);
			rechaza("suman", () -> confirmar());
		}

		@Test
		void aceptaCategoriasQueSuman100() {
			int otra = matricula(ana, "A2");
			categoria(otra, 1, 50);
			categoria(otra, 2, 50);
			assertThatCode(() -> confirmar()).doesNotThrowAnyException();
		}

		@Test
		void rechazaBorrarUnaCategoriaQueDejaLaSumaEnMenosDe100() {
			jdbc.update("DELETE FROM actividad_evaluativa WHERE id_matricula = ? AND consec_categoria = 3", matriculaDeAna);
			jdbc.update("DELETE FROM categoria_evaluacion WHERE id_matricula = ? AND consec_categoria = 3", matriculaDeAna);
			rechaza("suman", () -> confirmar());
		}
	}

	@Nested
	class Restriccion2_ActividadesSuman100 {
		@Test
		void rechazaActividadesQueSuman90() {
			jdbc.update("UPDATE actividad_evaluativa SET porcentaje = 50 WHERE id_matricula = ? AND consec_categoria = 1", matriculaDeAna);
			actividad(matriculaDeAna, 1, 2, 40);
			rechaza("suman", () -> confirmar());
		}

		@Test
		void aceptaActividadesQueSuman100() {
			jdbc.update("UPDATE actividad_evaluativa SET porcentaje = 50 WHERE id_matricula = ? AND consec_categoria = 1", matriculaDeAna);
			actividad(matriculaDeAna, 1, 2, 50);
			assertThatCode(() -> confirmar()).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion3_ItemsDePlantillaSuman100 {
		@Test
		void rechazaItemsQueSuman90() {
			int plantilla = nuevaPlantilla("Prueba");
			item(plantilla, 1, 50);
			item(plantilla, 2, 40);
			rechaza("suman", () -> confirmar());
		}

		@Test
		void aceptaItemsQueSuman100() {
			int plantilla = nuevaPlantilla("Prueba");
			item(plantilla, 1, 50);
			item(plantilla, 2, 50);
			assertThatCode(() -> confirmar()).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion4_MatriculaUnica {
		@Test
		void rechazaLaMismaAsignaturaDelMismoEstudianteEnElMismoPeriodo() {
			rechaza("uk_matricula_estudiante_asignatura_periodo", () -> matricula(ana, "A1"));
		}

		@Test
		void aceptaLaMismaAsignaturaParaOtroEstudiante() {
			assertThatCode(() -> matricula(luis, "A1")).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion5_PrerrequisitosSinCiclos {
		@Test
		void rechazaUnaAsignaturaComoPrerrequisitoDeSiMisma() {
			rechaza("ck_prerreq_no_a_si_misma", () -> prerrequisito("A1", "A1", "prerrequisito"));
		}

		@Test
		void rechazaUnCicloDirecto() {
			prerrequisito("A2", "A1", "prerrequisito");
			rechaza("ciclo", () -> prerrequisito("A1", "A2", "prerrequisito"));
		}

		@Test
		void rechazaUnCicloTransitivo() {
			prerrequisito("A3", "A2", "prerrequisito");
			prerrequisito("A2", "A1", "prerrequisito");
			rechaza("ciclo", () -> prerrequisito("A1", "A3", "prerrequisito"));
		}

		@Test
		void permiteCorrequisitosMutuos() {
			assertThatCode(() -> {
				prerrequisito("A1", "A2", "correquisito");
				prerrequisito("A2", "A1", "correquisito");
			}).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion6_CalificacionesEntreCeroYCinco {
		@Test
		void rechazaNotasFueraDeRango() {
			marcarComoCalificada(matriculaDeAna, 1, 1);
			rechaza("ck_calificacion_nota", () -> calificar(matriculaDeAna, 1, 1, "5.5"));
			rechaza("ck_calificacion_nota", () -> calificar(matriculaDeAna, 1, 1, "-0.1"));
		}

		@Test
		void aceptaLosExtremos() {
			marcarComoCalificada(matriculaDeAna, 1, 1);
			marcarComoCalificada(matriculaDeAna, 2, 1);
			assertThatCode(() -> {
				calificar(matriculaDeAna, 1, 1, "0.0");
				calificar(matriculaDeAna, 2, 1, "5.0");
			}).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion7_HoraDeInicioAnteriorALaDeFin {
		@Test
		void rechazaInicioPosteriorOIgualAlFin() {
			rechaza("ck_bloque_horas", () -> bloque(matriculaDeAna, "10:00", "08:00"));
			rechaza("ck_bloque_horas", () -> bloque(matriculaDeAna, "08:00", "08:00"));
		}

		@Test
		void aceptaUnBloqueValido() {
			assertThatCode(() -> bloque(matriculaDeAna, "08:00", "10:00")).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion8_SinCalificacionSiNoSeEntrego {
		@Test
		void rechazaCalificarUnaActividadNoEntregada() {
			rechaza("no entregada", () -> calificar(matriculaDeAna, 1, 1, "4.0"));
		}

		@Test
		void aceptaCalificarUnaActividadEntregada() {
			marcarComoCalificada(matriculaDeAna, 1, 1);
			assertThatCode(() -> calificar(matriculaDeAna, 1, 1, "4.0")).doesNotThrowAnyException();
		}

		@Test
		void rechazaVolverANoEntregadaUnaActividadConNota() {
			marcarComoCalificada(matriculaDeAna, 1, 1);
			calificar(matriculaDeAna, 1, 1, "4.0");
			rechaza("no entregada", () -> jdbc.update(
					"UPDATE actividad_evaluativa SET estado_entrega = 'no_entregada' WHERE id_matricula = ? AND consec_categoria = 1 AND consec_actividad = 1",
					matriculaDeAna));
		}
	}

	@Nested
	class Restriccion9_MetodosDeAcceso {
		@Test
		void rechazaUnMetodoLocalSinContrasenaOConIdentificadorExterno() {
			int nuevo = jdbc.queryForObject("INSERT INTO estudiante (nombres, apellidos, correo_institucional) VALUES ('N', 'N', 'n@ucundinamarca.edu.co') RETURNING id_estudiante", Integer.class);
			rechaza("ck_credencial_coherente", () -> jdbc.update(
					"INSERT INTO credencial_acceso (id_estudiante, proveedor, correo_proveedor) VALUES (?, 'local', 'n@ucundinamarca.edu.co')", nuevo));
			rechaza("ck_credencial_coherente", () -> jdbc.update(
					"INSERT INTO credencial_acceso (id_estudiante, proveedor, hash_contrasena, identificador_externo, correo_proveedor) VALUES (?, 'local', ?, 'g-1', 'n@ucundinamarca.edu.co')", nuevo, HASH));
		}

		@Test
		void rechazaUnMetodoFederadoSinIdentificadorOConContrasena() {
			rechaza("ck_credencial_coherente", () -> jdbc.update(
					"INSERT INTO credencial_acceso (id_estudiante, proveedor, correo_proveedor) VALUES (?, 'google', 'a@gmail.com')", ana));
			rechaza("ck_credencial_coherente", () -> jdbc.update(
					"INSERT INTO credencial_acceso (id_estudiante, proveedor, hash_contrasena, identificador_externo, correo_proveedor) VALUES (?, 'google', ?, 'g-1', 'a@gmail.com')", ana, HASH));
		}

		@Test
		void rechazaDesactivarElUnicoMetodoActivo() {
			jdbc.update("UPDATE credencial_acceso SET activa = FALSE WHERE id_estudiante = ?", ana);
			rechaza("método de acceso activo", () -> confirmar());
		}

		@Test
		void rechazaBorrarElUnicoMetodoDeAcceso() {
			jdbc.update("DELETE FROM credencial_acceso WHERE id_estudiante = ?", ana);
			rechaza("método de acceso activo", () -> confirmar());
		}

		@Test
		void rechazaUnaCuentaNuevaSinNingunMetodo() {
			jdbc.update("INSERT INTO estudiante (nombres, apellidos, correo_institucional) VALUES ('N', 'N', 'n@ucundinamarca.edu.co')");
			rechaza("método de acceso activo", () -> confirmar());
		}

		@Test
		void permiteDesactivarUnMetodoSiQuedaOtroActivo() {
			vincularGoogle(ana, "g-ana");
			jdbc.update("UPDATE credencial_acceso SET activa = FALSE WHERE id_estudiante = ? AND proveedor = 'local'", ana);
			assertThatCode(() -> confirmar()).doesNotThrowAnyException();
		}

		@Test
		void borrarLaCuentaBorraTodoLoSuyo() {
			assertThatCode(() -> {
				jdbc.update("DELETE FROM estudiante WHERE id_estudiante = ?", ana);
				confirmar();
			}).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion10_IdentificadorExternoUnico {
		@Test
		void rechazaElMismoGoogleEnDosCuentas() {
			vincularGoogle(ana, "g-compartido");
			rechaza("uk_credencial_identificador_externo", () -> vincularGoogle(luis, "g-compartido"));
		}

		@Test
		void rechazaElMismoCorreoConOtraCapitalizacion() {
			rechaza("uk_estudiante_correo", () -> jdbc.update(
					"INSERT INTO estudiante (nombres, apellidos, correo_institucional) VALUES ('X', 'X', 'ANA@UCUNDINAMARCA.EDU.CO')"));
		}
	}

	@Nested
	class Restriccion11_Sesiones {
		@Test
		void rechazaUnaHuellaRepetida() {
			sesion(ana, 1, HUELLA_A);
			rechaza("uk_sesion_hash_token", () -> sesion(luis, 1, HUELLA_A));
		}

		@Test
		void aceptaRevocarUnaSesionVigenteYRotarSuToken() {
			sesion(ana, 1, HUELLA_A);
			assertThatCode(() -> jdbc.update("UPDATE sesion SET hash_token_refresco = ? WHERE id_estudiante = ? AND consec_sesion = 1", HUELLA_B, ana))
					.doesNotThrowAnyException();
			assertThatCode(() -> revocar(ana, 1)).doesNotThrowAnyException();
		}

		@Test
		void rechazaQueUnaSesionRevocadaVuelvaAEstarVigente() {
			sesion(ana, 1, HUELLA_A);
			revocar(ana, 1);
			rechaza("ya fue revocada", () -> jdbc.update(
					"UPDATE sesion SET fecha_revocacion = NULL, motivo_revocacion = NULL WHERE id_estudiante = ? AND consec_sesion = 1", ana));
		}

		@Test
		void rechazaQueUnaSesionRevocadaRoteSuToken() {
			sesion(ana, 1, HUELLA_A);
			revocar(ana, 1);
			rechaza("ya fue revocada", () -> jdbc.update(
					"UPDATE sesion SET hash_token_refresco = ? WHERE id_estudiante = ? AND consec_sesion = 1", HUELLA_B, ana));
		}
	}

	@Nested
	class Restriccion12_UnSoloOrigenPorAviso {
		@Test
		void rechazaAvisosConMasDeUnOrigen() {
			int pendiente = pendiente(ana, null, null, null);
			rechaza("ck_notificacion_un_solo_origen", () -> aviso(ana, pendiente, matriculaDeAna, 1, 1));
			rechaza("ck_notificacion_un_solo_origen", () -> aviso(ana, pendiente, matriculaDeAna, null, null));
		}

		@Test
		void aceptaCadaOrigenPorSeparado() {
			int pendiente = pendiente(ana, null, null, null);
			assertThatCode(() -> {
				aviso(ana, pendiente, null, null, null);
				aviso(ana, null, matriculaDeAna, 1, 1);
				aviso(ana, null, matriculaDeAna, null, null);
				aviso(ana, null, null, null, null);
			}).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion13_PendienteConSuMatricula {
		@Test
		void rechazaUnaCategoriaSinMatricula() {
			rechaza("ck_pendiente_origen", () -> pendiente(ana, null, 1, null));
		}

		@Test
		void rechazaUnaActividadSinCategoria() {
			rechaza("ck_pendiente_origen", () -> pendiente(ana, matriculaDeAna, null, 1));
		}

		@Test
		void rechazaUnaActividadQueNoExisteEnEsaMatricula() {
			rechaza("fk_pendiente_actividad", () -> pendiente(ana, matriculaDeAna, 1, 9));
		}

		@Test
		void aceptaLasFormasValidas() {
			assertThatCode(() -> {
				pendiente(ana, null, null, null);
				pendiente(ana, matriculaDeAna, null, null);
				pendiente(ana, matriculaDeAna, 1, 1);
			}).doesNotThrowAnyException();
		}
	}

	@Nested
	class Restriccion14_SimulacionDeLaMismaMatricula {
		@Test
		void aceptaUnaActividadDeLaMatriculaSimulada() {
			int simulacion = simulacion(matriculaDeAna);
			assertThatCode(() -> detalle(simulacion, matriculaDeAna, 1, 1, "4.5")).doesNotThrowAnyException();
		}

		@Test
		void rechazaUnDetalleQueDeclaraOtraMatricula() {
			int simulacion = simulacion(matriculaDeAna);
			int deLuis = matricula(luis, "A1");
			estructuraDeEvaluacion(deLuis);
			rechaza("fk_detalle_simulacion", () -> detalle(simulacion, deLuis, 1, 1, "4.5"));
		}

		@Test
		void rechazaUnaActividadQueNoExisteEnLaMatriculaSimulada() {
			int simulacion = simulacion(matriculaDeAna);
			rechaza("fk_detalle_actividad", () -> detalle(simulacion, matriculaDeAna, 1, 9, "4.5"));
		}
	}

	// ------------------------------------------------------------------ ayudas

	private void categoria(int matricula, int consec, int porcentaje) {
		jdbc.update("INSERT INTO categoria_evaluacion (id_matricula, consec_categoria, nombre_categoria, porcentaje) VALUES (?, ?, 'Categoría', ?)",
				matricula, consec, porcentaje);
	}

	private void actividad(int matricula, int categoria, int consec, int porcentaje) {
		jdbc.update("INSERT INTO actividad_evaluativa (id_matricula, consec_categoria, consec_actividad, nombre_actividad, porcentaje) VALUES (?, ?, ?, 'Actividad', ?)",
				matricula, categoria, consec, porcentaje);
	}

	private int nuevaPlantilla(String nombre) {
		return jdbc.queryForObject("INSERT INTO plantilla_evaluacion (nombre_plantilla) VALUES (?) RETURNING id_plantilla", Integer.class, nombre);
	}

	private void item(int plantilla, int consec, int porcentaje) {
		jdbc.update("INSERT INTO item_de_plantilla (id_plantilla, consec_item, nombre_item, porcentaje) VALUES (?, ?, 'Ítem', ?)", plantilla, consec, porcentaje);
	}

	private void prerrequisito(String asignatura, String requerida, String tipo) {
		jdbc.update("INSERT INTO prerrequisito VALUES (?, ?, ?)", asignatura, requerida, tipo);
	}

	private void calificar(int matricula, int categoria, int actividad, String nota) {
		jdbc.update("INSERT INTO calificacion (id_matricula, consec_categoria, consec_actividad, nota_obtenida) VALUES (?, ?, ?, ?::numeric)",
				matricula, categoria, actividad, nota);
	}

	private void bloque(int matricula, String inicio, String fin) {
		jdbc.update("INSERT INTO bloque_de_horario (id_matricula, consec_bloque, dia_semana, hora_inicio, hora_fin) VALUES (?, 1, 'lunes', ?::time, ?::time)",
				matricula, inicio, fin);
	}

	private void vincularGoogle(int estudiante, String identificador) {
		jdbc.update("INSERT INTO credencial_acceso (id_estudiante, proveedor, identificador_externo, correo_proveedor) VALUES (?, 'google', ?, 'x@gmail.com')",
				estudiante, identificador);
	}

	private void sesion(int estudiante, int consec, String huella) {
		jdbc.update("INSERT INTO sesion (id_estudiante, consec_sesion, proveedor_origen, hash_token_refresco, fecha_expiracion) VALUES (?, ?, 'local', ?, now() + interval '7 days')",
				estudiante, consec, huella);
	}

	private void revocar(int estudiante, int consec) {
		jdbc.update("UPDATE sesion SET fecha_revocacion = now(), motivo_revocacion = 'cierre_sesion' WHERE id_estudiante = ? AND consec_sesion = ?",
				estudiante, consec);
	}

	private int pendiente(int estudiante, Integer matricula, Integer categoria, Integer actividad) {
		return jdbc.queryForObject("""
				INSERT INTO pendiente (id_estudiante, id_matricula, consec_categoria, consec_actividad, titulo, fecha_limite)
				VALUES (?, ?, ?, ?, 'Pendiente', now() + interval '2 days') RETURNING id_pendiente""",
				Integer.class, estudiante, matricula, categoria, actividad);
	}

	private void aviso(int estudiante, Integer pendiente, Integer matricula, Integer categoria, Integer actividad) {
		jdbc.update("""
				INSERT INTO notificacion (id_estudiante, id_pendiente, id_matricula, consec_categoria, consec_actividad, tipo_notificacion, titulo, mensaje)
				VALUES (?, ?, ?, ?, ?, 'vencimiento_proximo', 'Aviso', 'Mensaje')""",
				estudiante, pendiente, matricula, categoria, actividad);
	}

	private int simulacion(int matricula) {
		return jdbc.queryForObject("INSERT INTO simulacion (id_matricula, meta_utilizada) VALUES (?, 3.0) RETURNING id_simulacion", Integer.class, matricula);
	}

	private void detalle(int simulacion, int matricula, int categoria, int actividad, String nota) {
		jdbc.update("INSERT INTO detalle_simulacion VALUES (?, 1, ?, ?, ?, ?::numeric)", simulacion, matricula, categoria, actividad, nota);
	}
}
