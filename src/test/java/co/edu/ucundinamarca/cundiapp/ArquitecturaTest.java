package co.edu.ucundinamarca.cundiapp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regla 1 de la guía: el dominio es Java puro y las dependencias apuntan hacia adentro. Si una
 * de estas reglas falla, la compilación se rompe: una arquitectura que no se verifica solo
 * existe en el nombre de las carpetas. Se analiza el código de producción: las pruebas arman dobles
 * y configuraciones que cruzan capas a propósito (por ejemplo, TestcontainersConfiguration).
 */
@AnalyzeClasses(packages = "co.edu.ucundinamarca.cundiapp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

	@ArchTest
	static final ArchRule dominioPuro = noClasses().that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..",
					"..application..", "..infrastructure..");

	@ArchTest
	static final ArchRule aplicacionSinSpring = noClasses().that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "..infrastructure..");

	@ArchTest
	static final ArchRule capas = layeredArchitecture().consideringAllDependencies()
			.layer("Dominio").definedBy("..domain..")
			.layer("Aplicacion").definedBy("..application..")
			.layer("Infraestructura").definedBy("..infrastructure..")
			.whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer()
			.whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Infraestructura")
			.whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "Infraestructura");
}
