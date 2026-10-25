package co.edu.ucundinamarca.cundiapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.util.StreamUtils;

/**
 * SCRUM-91 (ADR 0008): Row-Level Security como segunda capa de defensa. El backend conecta como dueño de las
 * tablas y no pasa por RLS; cualquier otro rol queda sin acceso, y aunque se le dieran permisos leería 0 filas.
 */
class RowLevelSecurityTest extends PruebaConBaseDeDatos {

	private static final String MIGRACION = "db/migration/V17__row_level_security.sql";
	private static final List<String> ROLES_DE_LA_API = List.of("anon", "authenticated", "service_role");

	@Test
	void todaTablaDelEsquemaTieneRlsActiva() {
		List<String> sinRls = jdbc.queryForList("""
				SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
				WHERE n.nspname = 'cundiapp' AND c.relkind = 'r' AND NOT c.relrowsecurity
				  AND c.relname <> 'flyway_schema_history' ORDER BY 1""", String.class);

		assertThat(sinRls).as("tablas de cundiapp sin Row-Level Security").isEmpty();
	}

	@Test
	void lasVistasSeConsultanConLosPermisosDeQuienPregunta() {
		List<String> sinInvoker = jdbc.queryForList("""
				SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
				WHERE n.nspname = 'cundiapp' AND c.relkind = 'v'
				  AND coalesce('security_invoker=true' = ANY (c.reloptions), false) = false ORDER BY 1""", String.class);

		assertThat(sinInvoker).as("vistas sin security_invoker").isEmpty();
	}

	@Test
	void unRolSinPermisosNoLeeNada() {
		jdbc.execute("CREATE ROLE sin_permisos NOLOGIN");

		comoRol("sin_permisos", () ->
				assertThatThrownBy(() -> jdbc.queryForObject("SELECT count(*) FROM cundiapp.estudiante", Integer.class))
						.isInstanceOf(DataAccessException.class)
						.rootCause()
						.hasMessageContaining("permission denied"));
	}

	@Test
	void unRolConSelectPeroSinPoliticaLee0Filas() {
		catalogoBasico();
		cuenta("ana@ucundinamarca.edu.co");
		assertThat(contar("SELECT count(*) FROM estudiante")).as("el dueño sí ve filas").isPositive();
		jdbc.execute("CREATE ROLE lector NOLOGIN");
		jdbc.execute("GRANT USAGE ON SCHEMA cundiapp TO lector");
		jdbc.execute("GRANT SELECT ON cundiapp.estudiante TO lector");

		comoRol("lector", () ->
				assertThat(jdbc.queryForObject("SELECT count(*) FROM cundiapp.estudiante", Integer.class)).isZero());
	}

	@Test
	void laMigracionLeQuitaLosPermisosALosRolesDeLaApiDeSupabase() throws IOException {
		crearRolesDeLaApi();
		jdbc.execute("GRANT USAGE ON SCHEMA cundiapp TO anon, authenticated, service_role");
		jdbc.execute("GRANT ALL ON ALL TABLES IN SCHEMA cundiapp TO anon, authenticated, service_role");
		jdbc.execute("GRANT ALL ON ALL SEQUENCES IN SCHEMA cundiapp TO anon, authenticated, service_role");
		jdbc.execute("GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA cundiapp TO anon, authenticated, service_role");

		aplicarLaMigracion();

		for (String rol : ROLES_DE_LA_API) {
			assertThat(tiene(rol, "has_schema_privilege(?, 'cundiapp', 'USAGE')")).as(rol + " usa el esquema").isFalse();
			assertThat(tiene(rol, "has_table_privilege(?, 'cundiapp.estudiante', 'SELECT')")).as(rol + " lee").isFalse();
			assertThat(tiene(rol, "has_table_privilege(?, 'cundiapp.v_estado_asignatura', 'SELECT')"))
					.as(rol + " lee vistas").isFalse();
			assertThat(tiene(rol, "has_function_privilege(?, 'cundiapp.marcar_actualizacion()', 'EXECUTE')"))
					.as(rol + " ejecuta funciones").isFalse();
		}
	}

	@Test
	void lasTablasQueNazcanDespuesNacenCerradas() throws IOException {
		crearRolesDeLaApi();
		aplicarLaMigracion();

		jdbc.execute("CREATE TABLE cundiapp.prueba_futura (id INTEGER PRIMARY KEY)");

		for (String rol : ROLES_DE_LA_API) {
			assertThat(tiene(rol, "has_table_privilege(?, 'cundiapp.prueba_futura', 'SELECT')"))
					.as(rol + " lee una tabla nueva").isFalse();
		}
	}

	@Test
	void siElLatidoExisteLaMigracionLoCierraSinBorrarlo() throws IOException {
		crearRolesDeLaApi();
		jdbc.execute("CREATE TABLE public.latido (id INTEGER PRIMARY KEY)");
		jdbc.execute("INSERT INTO public.latido VALUES (1)");
		jdbc.execute("CREATE FUNCTION public.latir() RETURNS integer LANGUAGE sql AS 'SELECT 1'");
		jdbc.execute("GRANT SELECT ON public.latido TO anon, authenticated, service_role");
		jdbc.execute("GRANT EXECUTE ON FUNCTION public.latir() TO anon, authenticated, service_role");

		aplicarLaMigracion();

		assertThat(contar("SELECT count(*) FROM public.latido")).as("no se borra nada").isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT relrowsecurity FROM pg_class WHERE oid = 'public.latido'::regclass",
				Boolean.class)).isTrue();
		for (String rol : ROLES_DE_LA_API) {
			assertThat(tiene(rol, "has_table_privilege(?, 'public.latido', 'SELECT')")).as(rol + " lee latido").isFalse();
			assertThat(tiene(rol, "has_function_privilege(?, 'public.latir()', 'EXECUTE')"))
					.as(rol + " ejecuta latir").isFalse();
		}
	}

	@Test
	void sinRolesDeSupabaseNiLatidoLaMigracionCorreSinTocarNada() throws IOException {
		aplicarLaMigracion();

		assertThat(contar("SELECT count(*) FROM pg_roles WHERE rolname IN ('anon', 'authenticated', 'service_role')"))
				.isZero();
	}

	private void crearRolesDeLaApi() {
		for (String rol : ROLES_DE_LA_API) {
			jdbc.execute("CREATE ROLE " + rol + " NOLOGIN");
		}
	}

	/** La migración es idempotente: volver a correrla es la forma de probarla con los roles de Supabase presentes. */
	private void aplicarLaMigracion() throws IOException {
		try (InputStream script = new ClassPathResource(MIGRACION).getInputStream()) {
			jdbc.execute(StreamUtils.copyToString(script, StandardCharsets.UTF_8));
		}
	}

	private boolean tiene(String rol, String consulta) {
		return Boolean.TRUE.equals(jdbc.queryForObject("SELECT " + consulta, Boolean.class, rol));
	}

	/**
	 * Ejecuta la comprobación con los permisos de otro rol. Un rechazo deja la transacción abortada, así que todo va
	 * dentro de un punto de recuperación: al volver a él se recupera la transacción y también el rol del dueño.
	 */
	private void comoRol(String rol, Runnable comprobacion) {
		jdbc.execute("SAVEPOINT antes_del_rol");
		jdbc.execute("SET LOCAL ROLE " + rol);
		try {
			comprobacion.run();
		} finally {
			jdbc.execute("ROLLBACK TO SAVEPOINT antes_del_rol");
		}
	}
}
