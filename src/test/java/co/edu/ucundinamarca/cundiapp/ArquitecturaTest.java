package co.edu.ucundinamarca.cundiapp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regla 1 de la guía: el dominio es Java puro y las dependencias apuntan hacia adentro. Si una
 * de estas reglas falla, la compilación se rompe: una arquitectura que no se verifica solo
 * existe en el nombre de las carpetas.
 */
@AnalyzeClasses(packages = "co.edu.ucundinamarca.cundiapp")
class ArquitecturaTest {

	@ArchTest
	static final ArchRule dominioPuro = noClasses().that().resideInAPackage("..dominio..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..",
					"..aplicacion..", "..infraestructura..");

	@ArchTest
	static final ArchRule aplicacionSinSpring = noClasses().that().resideInAPackage("..aplicacion..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "..infraestructura..");

	@ArchTest
	static final ArchRule capas = layeredArchitecture().consideringAllDependencies()
			.layer("Dominio").definedBy("..dominio..")
			.layer("Aplicacion").definedBy("..aplicacion..")
			.layer("Infraestructura").definedBy("..infraestructura..")
			.whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer()
			.whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Infraestructura")
			.whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "Infraestructura");
}
