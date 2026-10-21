package co.edu.ucundinamarca.cundiapp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.Comparator;

/**
 * Los principios SOLID, vigilados en cada compilación (SCRUM-238). {@link ArquitecturaTest} cuida que las capas apunten hacia adentro; estas
 * reglas cuidan lo que pasa <em>dentro</em> de cada capa, que es donde SOLID se desgasta sin que nadie lo note: un servicio que crece hasta
 * hacer de todo, un puerto que se va llenando de métodos, un controlador que se salta el puerto y llama a un adaptador.
 *
 * <p>La arquitectura hexagonal ya los cumple por construcción (un puerto por caso de uso, dependencias hacia adentro); esto solo impide que
 * se pierdan. Si una regla falla, lo normal es corregir el código. Una excepción se documenta aquí mismo, con el motivo y quién la revisa.
 *
 * <p>Lo que ArchUnit no puede ver: el principio abierto/cerrado (OCP) en su forma de {@code if/instanceof} sobre tipos, que no deja rastro
 * en las dependencias. Se cuida por otro lado: hoy no hay ningún {@code instanceof} en {@code domain} ni en {@code application}, y la
 * extensión entra por puertos nuevos, no por ramas nuevas.
 */
@AnalyzeClasses(packages = "co.edu.ucundinamarca.cundiapp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaSolidTest {

	private static final String PUERTOS_DE_ENTRADA = "..application.port.in..";
	private static final String PUERTOS_DE_SALIDA = "..application.port.out..";
	private static final String SERVICIOS = "..application.service..";

	/** Cuántas operaciones puede declarar un puerto de entrada: un caso de uso, a lo sumo con su variante. */
	private static final int OPERACIONES_POR_PUERTO_DE_ENTRADA = 2;
	/** Los repositorios son los puertos más anchos hoy (12 y 11); el tope impide que sigan creciendo. */
	private static final int OPERACIONES_POR_PUERTO_DE_SALIDA = 12;
	/** Más dependencias que esto en un constructor es la señal clásica de una clase que hace demasiado. Hoy el máximo es 6. */
	private static final int DEPENDENCIAS_POR_CONSTRUCTOR = 7;

	// ---- S: responsabilidad unica ---------------------------------------------------------------------------------------------------

	/** Un servicio es un caso de uso: implementa exactamente un puerto de entrada. */
	@ArchTest
	static final ArchRule cadaServicioImplementaUnSoloPuertoDeEntrada = classes()
			.that().resideInAPackage(SERVICIOS).and().areTopLevelClasses().and().areNotInterfaces()
			.should(implementarExactamenteUnPuertoDeEntrada());

	@ArchTest
	static final ArchRule losServiciosNoTienenConstructoresDesmedidos = classes()
			.that().resideInAPackage(SERVICIOS).and().areTopLevelClasses()
			.should(tenerConstructoresDeComoMaximo(DEPENDENCIAS_POR_CONSTRUCTOR));

	// ---- L: sustitucion de Liskov ---------------------------------------------------------------------------------------------------

	/** Los servicios se componen, no se heredan: ninguno extiende a otra clase, así que ninguno puede romper lo que promete su padre. */
	@ArchTest
	static final ArchRule losServiciosNoHeredanDeNadie = classes()
			.that().resideInAPackage(SERVICIOS).and().areTopLevelClasses()
			.should(extenderSoloDeObject());

	// ---- I: segregacion de interfaces -----------------------------------------------------------------------------------------------

	/** Los puertos son interfaces: el servicio depende de lo que necesita, no de una clase concreta. Los registros y enums son datos. */
	@ArchTest
	static final ArchRule losPuertosSonInterfacesORegistros = classes()
			.that().resideInAnyPackage(PUERTOS_DE_ENTRADA, PUERTOS_DE_SALIDA).and().areTopLevelClasses()
			.should().beInterfaces().orShould().beRecords().orShould().beEnums();

	@ArchTest
	static final ArchRule losPuertosDeEntradaSonPequenos = classes()
			.that().resideInAPackage(PUERTOS_DE_ENTRADA).and().areInterfaces()
			.should(declararComoMaximoOperaciones(OPERACIONES_POR_PUERTO_DE_ENTRADA));

	@ArchTest
	static final ArchRule losPuertosDeSalidaNoCrecenSinLimite = classes()
			.that().resideInAPackage(PUERTOS_DE_SALIDA).and().areInterfaces()
			.should(declararComoMaximoOperaciones(OPERACIONES_POR_PUERTO_DE_SALIDA));

	// ---- D: inversion de dependencias -----------------------------------------------------------------------------------------------

	/** Un servicio habla con otros por sus puertos, nunca con la clase concreta de otro servicio. */
	@ArchTest
	static final ArchRule losServiciosNoSeLlamanEntreSi = classes()
			.that().resideInAPackage(SERVICIOS)
			.should(noDependerDeOtroServicio());

	/**
	 * Los adaptadores de entrada (controladores, filtros, tareas) llegan al núcleo por un puerto de entrada: no conocen la clase de un servicio ni
	 * a los adaptadores de salida.
	 */
	@ArchTest
	static final ArchRule losAdaptadoresDeEntradaUsanSoloPuertosDeEntrada = noClasses()
			.that().resideInAPackage("..infrastructure.adapter.in..")
			.should().dependOnClassesThat().resideInAnyPackage(SERVICIOS, "..infrastructure.adapter.out..");

	@ArchTest
	static final ArchRule losAdaptadoresDeSalidaNoConocenALosDeEntrada = noClasses()
			.that().resideInAPackage("..infrastructure.adapter.out..")
			.should().dependOnClassesThat().resideInAPackage("..infrastructure.adapter.in..");

	/** Persistencia, seguridad, notificaciones... no se conocen entre sí: cada adaptador se puede cambiar sin tocar a los demás. */
	@ArchTest
	static final ArchRule losAdaptadoresDeSalidaNoSeLlamanEntreSi = slices()
			.matching("..infrastructure.adapter.out.(*)..").should().notDependOnEachOther();

	@ArchTest
	static final ArchRule sinInyeccionPorCampo = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

	@ArchTest
	static final ArchRule sinCiclosEnElDominio = slices().matching("..domain.(*)..").should().beFreeOfCycles();

	@ArchTest
	static final ArchRule sinCiclosEnLosAdaptadoresDeSalida = slices().matching("..infrastructure.adapter.out.(*)..").should().beFreeOfCycles();

	// ---- condiciones ----------------------------------------------------------------------------------------------------------------

	private static ArchCondition<JavaClass> implementarExactamenteUnPuertoDeEntrada() {
		return new ArchCondition<>("implementar exactamente un puerto de entrada") {
			@Override
			public void check(JavaClass clase, ConditionEvents eventos) {
				long puertos = clase.getRawInterfaces().stream()
						.filter(i -> i.getPackageName().contains(".application.port.in"))
						.count();
				if (puertos != 1) {
					eventos.add(SimpleConditionEvent.violated(clase,
							clase.getSimpleName() + " implementa " + puertos + " puertos de entrada (tiene que ser 1)"));
				}
			}
		};
	}

	private static ArchCondition<JavaClass> tenerConstructoresDeComoMaximo(int maximo) {
		return new ArchCondition<>("tener constructores de " + maximo + " parámetros como máximo") {
			@Override
			public void check(JavaClass clase, ConditionEvents eventos) {
				clase.getConstructors().stream()
						.max(Comparator.comparingInt(c -> c.getRawParameterTypes().size()))
						.filter(c -> c.getRawParameterTypes().size() > maximo)
						.ifPresent((JavaConstructor c) -> eventos.add(SimpleConditionEvent.violated(clase,
								clase.getSimpleName() + " recibe " + c.getRawParameterTypes().size() + " dependencias en el constructor (máximo "
										+ maximo + "): probablemente hace más de una cosa")));
			}
		};
	}

	private static ArchCondition<JavaClass> extenderSoloDeObject() {
		return new ArchCondition<>("no extender ninguna clase") {
			@Override
			public void check(JavaClass clase, ConditionEvents eventos) {
				clase.getRawSuperclass()
						.filter(padre -> !padre.isEquivalentTo(Object.class))
						.ifPresent(padre -> eventos.add(SimpleConditionEvent.violated(clase,
								clase.getSimpleName() + " extiende " + padre.getName())));
			}
		};
	}

	private static ArchCondition<JavaClass> declararComoMaximoOperaciones(int maximo) {
		return new ArchCondition<>("declarar " + maximo + " operaciones como máximo") {
			@Override
			public void check(JavaClass puerto, ConditionEvents eventos) {
				long operaciones = puerto.getMethods().stream()
						.filter((JavaMethod m) -> m.getModifiers().contains(JavaModifier.ABSTRACT) && !m.getModifiers().contains(JavaModifier.STATIC))
						.count();
				if (operaciones > maximo) {
					eventos.add(SimpleConditionEvent.violated(puerto,
							puerto.getSimpleName() + " declara " + operaciones + " operaciones (máximo " + maximo + "): conviene partirlo"));
				}
			}
		};
	}

	/** El nombre de la clase de nivel superior: {@code Servicio$Interna} cuenta como {@code Servicio}. */
	private static String claseDeNivelSuperior(JavaClass clase) {
		String nombre = clase.getName();
		int signo = nombre.indexOf('$');
		return signo < 0 ? nombre : nombre.substring(0, signo);
	}

	private static ArchCondition<JavaClass> noDependerDeOtroServicio() {
		return new ArchCondition<>("no depender de otro servicio") {
			@Override
			public void check(JavaClass clase, ConditionEvents eventos) {
				String propia = claseDeNivelSuperior(clase);
				for (Dependency dependencia : clase.getDirectDependenciesFromSelf()) {
					JavaClass destino = dependencia.getTargetClass();
					if (destino.getPackageName().contains(".application.service") && !claseDeNivelSuperior(destino).equals(propia)) {
						eventos.add(SimpleConditionEvent.violated(clase,
								clase.getSimpleName() + " depende del servicio " + destino.getSimpleName() + ": que lo use por su puerto"));
					}
				}
			}
		};
	}
}
