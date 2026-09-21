package co.edu.ucundinamarca.cundiapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** SCRUM-41: el esquema queda completo después de aplicar las migraciones. */
class EsquemaBaseDeDatosTest extends PruebaConBaseDeDatos {

	@Test
	void quedanCreadasLas27TablasDelModelo() {
		int tablas = contar("""
				SELECT count(*) FROM information_schema.tables
				 WHERE table_schema = 'cundiapp' AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'""");
		assertThat(tablas).isEqualTo(27);
	}

	@Test
	void quedanCreadosLos204CamposDelDiccionario() {
		int campos = contar("""
				SELECT count(*) FROM information_schema.columns c
				  JOIN information_schema.tables t USING (table_schema, table_name)
				 WHERE c.table_schema = 'cundiapp' AND t.table_type = 'BASE TABLE' AND c.table_name <> 'flyway_schema_history'""");
		assertThat(campos).isEqualTo(204);
	}

	@Test
	void quedanCreadasLasCuatroVistas() {
		List<String> vistas = jdbc.queryForList(
				"SELECT table_name FROM information_schema.views WHERE table_schema = 'cundiapp' ORDER BY table_name", String.class);
		assertThat(vistas).containsExactly("v_avance_carrera", "v_estado_asignatura", "v_nota_categoria", "v_sesion_vigente");
	}

	@Test
	void losDatosDeArranqueTraenLaPlantillaDeTresCortesQueSuma100() {
		assertThat(contar("SELECT count(*) FROM plantilla_evaluacion WHERE es_predeterminada")).isEqualTo(1);
		assertThat(contar("SELECT count(*) FROM item_de_plantilla")).isEqualTo(3);
		assertThat(contar("SELECT sum(porcentaje)::int FROM item_de_plantilla")).isEqualTo(100);
	}

	@Test
	void losDatosDeArranqueTraenLasCategoriasDeLaGuia() {
		List<String> categorias = jdbc.queryForList(
				"SELECT nombre_categoria FROM categoria_de_recurso ORDER BY orden", String.class);
		assertThat(categorias).containsExactly("Reglamentos", "Formatos", "Convocatorias");
	}

	@Test
	void laNotaRequeridaSeCalculaEnLaVistaYNoSeGuarda() {
		catalogoBasico();
		int estudiante = cuenta("ana@ucundinamarca.edu.co");
		jdbc.update("INSERT INTO configuracion_estudiante (id_estudiante) VALUES (?)", estudiante);
		int matricula = matricula(estudiante, "A1");
		estructuraDeEvaluacion(matricula);
		marcarComoCalificada(matricula, 1, 1);
		jdbc.update("INSERT INTO calificacion (id_matricula, consec_categoria, consec_actividad, nota_obtenida) VALUES (?, 1, 1, 4.0)", matricula);

		// El primer corte (30 %) se calificó con 4.0: aporta 1.2 puntos y queda 70 % por evaluar.
		var estado = jdbc.queryForMap(
				"SELECT nota_acumulada, porcentaje_pendiente, nota_requerida, nivel_riesgo FROM v_estado_asignatura WHERE id_matricula = ?", matricula);
		assertThat(((Number) estado.get("nota_acumulada")).doubleValue()).isEqualTo(1.2);
		assertThat(((Number) estado.get("porcentaje_pendiente")).doubleValue()).isEqualTo(70.0);
		// Meta 3.0: necesita (3.0 - 1.2) / 0.70 = 2.571... en lo que falta, riesgo bajo.
		assertThat(((Number) estado.get("nota_requerida")).doubleValue()).isBetween(2.57, 2.58);
		assertThat(estado.get("nivel_riesgo")).isEqualTo("bajo");
	}

	@Test
	void elAvanceDeCarreraSaleDeLosCreditosAprobados() {
		catalogoBasico();
		int estudiante = cuenta("ana@ucundinamarca.edu.co");
		jdbc.update("UPDATE estudiante SET codigo_plan = 'PL1' WHERE id_estudiante = ?", estudiante);
		int matricula = matricula(estudiante, "A1");
		jdbc.update("UPDATE matricula_asignatura SET estado_matricula = 'aprobada' WHERE id_matricula = ?", matricula);

		var avance = jdbc.queryForMap("SELECT creditos_aprobados, porcentaje_avance FROM v_avance_carrera WHERE id_estudiante = ?", estudiante);
		assertThat(((Number) avance.get("creditos_aprobados")).intValue()).isEqualTo(3);
		assertThat(((Number) avance.get("porcentaje_avance")).doubleValue()).isEqualTo(1.88);
	}
}
