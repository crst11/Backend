package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import co.edu.ucundinamarca.cundiapp.application.port.in.BuscarRecursosInstitucionales;
import co.edu.ucundinamarca.cundiapp.application.port.in.CerrarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiCuenta;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarVinculoConGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.in.DesvincularGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.in.EliminarCuenta;
import co.edu.ucundinamarca.cundiapp.application.port.in.IniciarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.IniciarSesionConGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.application.port.in.CerrarSesionEnDispositivo;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiPerfilAcademico;
import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarRegistroExtendido;
import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarReporteDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarCalendario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiAvanceDeInformacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiDia;
import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarEstadoDeMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.in.CrearMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.in.EliminarMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisPendientes;
import co.edu.ucundinamarca.cundiapp.application.port.out.PendienteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.service.CambiarEstadoDeMiPendienteServicio;
import co.edu.ucundinamarca.cundiapp.application.service.CrearMiPendienteServicio;
import co.edu.ucundinamarca.cundiapp.application.service.EliminarMiPendienteServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ListarMisPendientesServicio;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro;
import co.edu.ucundinamarca.cundiapp.application.port.in.AgregarClaseAlHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMiMeta;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiProyeccionDeNota;
import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiPreferenciaDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.GuardarMiPreferenciaDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHistorial;
import co.edu.ucundinamarca.cundiapp.application.port.in.DefinirMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.DeshacerImportacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisAsignaturasMatriculadas;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisImportaciones;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarPlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.application.port.in.ElegirMiPrograma;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisSesiones;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarProgramas;
import co.edu.ucundinamarca.cundiapp.application.port.in.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RestablecerContrasena;
import co.edu.ucundinamarca.cundiapp.application.port.in.SolicitarRecuperacionDeContrasena;
import co.edu.ucundinamarca.cundiapp.application.port.in.RenovarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.VerificarCorreo;
import co.edu.ucundinamarca.cundiapp.application.port.in.VincularGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.out.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciaDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteDeHorarioPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteHistorialAcademicoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HistorialAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.GeneradorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDeIntentosPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDePeticionesPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RecursoInstitucionalRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VinculoConGoogleRepositorio;
import co.edu.ucundinamarca.cundiapp.application.service.AbridorDeSesion;
import co.edu.ucundinamarca.cundiapp.application.service.BuscarRecursosInstitucionalesServicio;
import co.edu.ucundinamarca.cundiapp.application.service.CerrarSesionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiCuentaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarVinculoConGoogleServicio;
import co.edu.ucundinamarca.cundiapp.application.service.DesvincularGoogleServicio;
import co.edu.ucundinamarca.cundiapp.application.service.EliminarCuentaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.EmisorDeCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.application.service.IniciarSesionConGoogleServicio;
import co.edu.ucundinamarca.cundiapp.application.service.IniciarSesionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ListarCategoriasDeRecursoServicio;
import co.edu.ucundinamarca.cundiapp.application.service.RegistrarEstudianteServicio;
import co.edu.ucundinamarca.cundiapp.application.service.CerrarSesionEnDispositivoServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiPerfilAcademicoServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarPlanDeEstudiosServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ElegirMiProgramaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ListarMisSesionesServicio;
import co.edu.ucundinamarca.cundiapp.application.service.AnalizarRegistroExtendidoServicio;
import co.edu.ucundinamarca.cundiapp.application.service.AnalizarReporteDeHorarioServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarCalendarioServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiAvanceDeInformacionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiDiaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConfirmarImportacionDeHorarioServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConfirmarImportacionDeRegistroServicio;
import co.edu.ucundinamarca.cundiapp.application.service.AgregarClaseAlHorarioServicio;
import co.edu.ucundinamarca.cundiapp.application.service.CambiarMiMetaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiConfiguracionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiEstructuraDeEvaluacionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiProyeccionDeNotaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.SimularMiNotaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiHorarioServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiPreferenciaDeHorarioServicio;
import co.edu.ucundinamarca.cundiapp.application.service.GuardarMiPreferenciaDeHorarioServicio;
import co.edu.ucundinamarca.cundiapp.application.service.QuitarClaseDelHorarioServicio;
import co.edu.ucundinamarca.cundiapp.application.service.QuitarMiCalificacionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.RegistrarMiCalificacionServicio;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotaRequerida;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotas;
import co.edu.ucundinamarca.cundiapp.application.service.ConsultarMiHistorialServicio;
import co.edu.ucundinamarca.cundiapp.application.service.DefinirMiEstructuraDeEvaluacionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.DeshacerImportacionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ListarMisAsignaturasMatriculadasServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ListarMisImportacionesServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ListarProgramasServicio;
import co.edu.ucundinamarca.cundiapp.application.service.ReenviarCodigoServicio;
import co.edu.ucundinamarca.cundiapp.application.service.RestablecerContrasenaServicio;
import co.edu.ucundinamarca.cundiapp.application.service.SolicitarRecuperacionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.RenovarSesionServicio;
import co.edu.ucundinamarca.cundiapp.application.service.VerificarCorreoServicio;
import co.edu.ucundinamarca.cundiapp.application.service.VincularGoogleServicio;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification.EnvioDelCodigo;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Aquí Spring conoce la aplicación, no al revés: ensambla los casos de uso con sus puertos de salida. */
@Configuration
class CasosDeUsoConfig {

	@Bean
	ListarCategoriasDeRecurso listarCategoriasDeRecurso(CategoriaDeRecursoRepositorio repositorio) {
		return new ListarCategoriasDeRecursoServicio(repositorio);
	}

	@Bean
	BuscarRecursosInstitucionales buscarRecursosInstitucionales(RecursoInstitucionalRepositorio repositorio) {
		return new BuscarRecursosInstitucionalesServicio(repositorio);
	}

	@Bean
	ListarProgramas listarProgramas(ProgramaAcademicoRepositorio programas) {
		return new ListarProgramasServicio(programas);
	}

	@Bean
	ConsultarPlanDeEstudios consultarPlanDeEstudios(ProgramaAcademicoRepositorio programas) {
		return new ConsultarPlanDeEstudiosServicio(programas);
	}

	@Bean
	AnalizarRegistroExtendido analizarRegistroExtendido(
			FuenteHistorialAcademicoPort fuente,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas,
			HistorialAcademicoRepositorio historiales,
			LimitadorDePeticionesPort limitador) {
		return new AnalizarRegistroExtendidoServicio(fuente, estudiantes, programas, historiales, limitador);
	}

	@Bean
	ConfirmarImportacionDeRegistro confirmarImportacionDeRegistro(
			ImportacionRepositorio importaciones,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas) {
		return new ConfirmarImportacionDeRegistroServicio(importaciones, estudiantes, programas);
	}

	@Bean
	ConsultarMiAvanceDeInformacion consultarMiAvanceDeInformacion(
			ImportacionRepositorio importaciones, HorarioRepositorio horarios, EstructuraDeEvaluacionRepositorio evaluaciones) {
		return new ConsultarMiAvanceDeInformacionServicio(importaciones, horarios, evaluaciones);
	}

	@Bean
	ConsultarMiDia consultarMiDia(
			HorarioRepositorio horarios, EstructuraDeEvaluacionRepositorio evaluaciones, java.time.Clock reloj) {
		return new ConsultarMiDiaServicio(horarios, evaluaciones, reloj);
	}

	@Bean
	CrearMiPendiente crearMiPendiente(
			PendienteRepositorio pendientes, EstructuraDeEvaluacionRepositorio evaluaciones, java.time.Clock reloj) {
		return new CrearMiPendienteServicio(pendientes, evaluaciones, reloj);
	}

	@Bean
	ListarMisPendientes listarMisPendientes(PendienteRepositorio pendientes, java.time.Clock reloj) {
		return new ListarMisPendientesServicio(pendientes, reloj);
	}

	@Bean
	CambiarEstadoDeMiPendiente cambiarEstadoDeMiPendiente(PendienteRepositorio pendientes, java.time.Clock reloj) {
		return new CambiarEstadoDeMiPendienteServicio(pendientes, reloj);
	}

	@Bean
	EliminarMiPendiente eliminarMiPendiente(PendienteRepositorio pendientes) {
		return new EliminarMiPendienteServicio(pendientes);
	}

	@Bean
	ConsultarCalendario consultarCalendario(java.time.Clock reloj) {
		return new ConsultarCalendarioServicio(reloj);
	}

	@Bean
	AnalizarReporteDeHorario analizarReporteDeHorario(
			FuenteDeHorarioPort fuente,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas,
			ImportacionDeHorarioRepositorio horarios,
			LimitadorDePeticionesPort limitador) {
		return new AnalizarReporteDeHorarioServicio(fuente, estudiantes, programas, horarios, limitador);
	}

	@Bean
	ConfirmarImportacionDeHorario confirmarImportacionDeHorario(
			ImportacionDeHorarioRepositorio horarios,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas) {
		return new ConfirmarImportacionDeHorarioServicio(horarios, estudiantes, programas);
	}

	@Bean
	ListarMisImportaciones listarMisImportaciones(ImportacionRepositorio importaciones) {
		return new ListarMisImportacionesServicio(importaciones);
	}

	@Bean
	DeshacerImportacion deshacerImportacion(ImportacionRepositorio importaciones) {
		return new DeshacerImportacionServicio(importaciones);
	}

	@Bean
	ConsultarMiHorario consultarMiHorario(HorarioRepositorio horarios) {
		return new ConsultarMiHorarioServicio(horarios);
	}

	@Bean
	ConsultarMiPreferenciaDeHorario consultarMiPreferenciaDeHorario(PreferenciaDeHorarioRepositorio preferencias) {
		return new ConsultarMiPreferenciaDeHorarioServicio(preferencias);
	}

	@Bean
	GuardarMiPreferenciaDeHorario guardarMiPreferenciaDeHorario(PreferenciaDeHorarioRepositorio preferencias) {
		return new GuardarMiPreferenciaDeHorarioServicio(preferencias);
	}

	@Bean
	AgregarClaseAlHorario agregarClaseAlHorario(HorarioRepositorio horarios) {
		return new AgregarClaseAlHorarioServicio(horarios);
	}

	@Bean
	QuitarClaseDelHorario quitarClaseDelHorario(HorarioRepositorio horarios) {
		return new QuitarClaseDelHorarioServicio(horarios);
	}

	@Bean
	ListarMisAsignaturasMatriculadas listarMisAsignaturasMatriculadas(
			EstructuraDeEvaluacionRepositorio evaluaciones) {
		return new ListarMisAsignaturasMatriculadasServicio(evaluaciones);
	}

	@Bean
	CalculadoraDeNotas calculadoraDeNotas() {
		return new CalculadoraDeNotas();
	}

	@Bean
	ConsultarMiEstructuraDeEvaluacion consultarMiEstructuraDeEvaluacion(
			EstructuraDeEvaluacionRepositorio evaluaciones, CalculadoraDeNotas calculadora) {
		return new ConsultarMiEstructuraDeEvaluacionServicio(evaluaciones, calculadora);
	}

	@Bean
	RegistrarMiCalificacion registrarMiCalificacion(
			EstructuraDeEvaluacionRepositorio evaluaciones, CalculadoraDeNotas calculadora) {
		return new RegistrarMiCalificacionServicio(evaluaciones, calculadora);
	}

	@Bean
	QuitarMiCalificacion quitarMiCalificacion(
			EstructuraDeEvaluacionRepositorio evaluaciones, CalculadoraDeNotas calculadora) {
		return new QuitarMiCalificacionServicio(evaluaciones, calculadora);
	}

	@Bean
	DefinirMiEstructuraDeEvaluacion definirMiEstructuraDeEvaluacion(
			EstructuraDeEvaluacionRepositorio evaluaciones) {
		return new DefinirMiEstructuraDeEvaluacionServicio(evaluaciones);
	}

	@Bean
	CalculadoraDeNotaRequerida calculadoraDeNotaRequerida(CalculadoraDeNotas calculadora) {
		return new CalculadoraDeNotaRequerida(calculadora);
	}

	@Bean
	SimularMiNota simularMiNota(
			EstructuraDeEvaluacionRepositorio evaluaciones,
			ConfiguracionRepositorio configuracion,
			CalculadoraDeNotaRequerida calculadora) {
		return new SimularMiNotaServicio(evaluaciones, configuracion, calculadora);
	}

	@Bean
	ConsultarMiProyeccionDeNota consultarMiProyeccionDeNota(SimularMiNota simulador) {
		return new ConsultarMiProyeccionDeNotaServicio(simulador);
	}

	@Bean
	ConsultarMiConfiguracion consultarMiConfiguracion(ConfiguracionRepositorio configuracion) {
		return new ConsultarMiConfiguracionServicio(configuracion);
	}

	@Bean
	CambiarMiMeta cambiarMiMeta(ConfiguracionRepositorio configuracion) {
		return new CambiarMiMetaServicio(configuracion);
	}

	@Bean
	ConsultarMiHistorial consultarMiHistorial(
			HistorialAcademicoRepositorio historiales,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas) {
		return new ConsultarMiHistorialServicio(historiales, estudiantes, programas);
	}

	@Bean
	ConsultarMiPerfilAcademico consultarMiPerfilAcademico(
			EstudianteRepositorio estudiantes, ProgramaAcademicoRepositorio programas) {
		return new ConsultarMiPerfilAcademicoServicio(estudiantes, programas);
	}

	@Bean
	ElegirMiPrograma elegirMiPrograma(
			EstudianteRepositorio estudiantes, ProgramaAcademicoRepositorio programas) {
		return new ElegirMiProgramaServicio(estudiantes, programas);
	}

	@Bean
	ListarMisSesiones listarMisSesiones(SesionRepositorio sesiones, RelojPort reloj) {
		return new ListarMisSesionesServicio(sesiones, reloj);
	}

	@Bean
	CerrarSesionEnDispositivo cerrarSesionEnDispositivo(SesionRepositorio sesiones, RelojPort reloj) {
		return new CerrarSesionEnDispositivoServicio(sesiones, reloj);
	}

	@Bean
	SolicitarRecuperacionDeContrasena solicitarRecuperacionDeContrasena(
			EstudianteRepositorio estudiantes,
			CodigoDeVerificacionRepositorio codigos,
			EmisorDeCodigoDeVerificacion emisor,
			RelojPort reloj) {
		return new SolicitarRecuperacionServicio(estudiantes, codigos, emisor, reloj);
	}

	@Bean
	RestablecerContrasena restablecerContrasena(
			EstudianteRepositorio estudiantes,
			CodigoDeVerificacionRepositorio codigos,
			SesionRepositorio sesiones,
			CifradorDeContrasenaPort cifrador,
			RelojPort reloj) {
		return new RestablecerContrasenaServicio(estudiantes, codigos, sesiones, cifrador, reloj);
	}

	@Bean
	EmisorDeCodigoDeVerificacion emisorDeCodigoDeVerificacion(
			GeneradorDeCodigoPort generador,
			// El decorador que envía fuera de la petición (SCRUM-67); él decide a quién envuelve.
			@Qualifier(EnvioDelCodigo.ASINCRONO) EnviadorDeCodigoPort enviador,
			CodigoDeVerificacionRepositorio codigos,
			RelojPort reloj) {
		return new EmisorDeCodigoDeVerificacion(generador, enviador, codigos, reloj);
	}

	@Bean
	RegistrarEstudiante registrarEstudiante(
			EstudianteRepositorio repositorio,
			CifradorDeContrasenaPort cifrador,
			RelojPort reloj,
			EmisorDeCodigoDeVerificacion emisor) {
		return new RegistrarEstudianteServicio(repositorio, cifrador, reloj, emisor);
	}

	@Bean
	VerificarCorreo verificarCorreo(
			EstudianteRepositorio estudiantes, CodigoDeVerificacionRepositorio codigos, RelojPort reloj) {
		return new VerificarCorreoServicio(estudiantes, codigos, reloj);
	}

	@Bean
	AbridorDeSesion abridorDeSesion(
			SesionRepositorio sesiones,
			EmisorDeTokensPort tokens,
			@Value("${cundiapp.jwt.duracion-acceso}") Duration vigenciaAcceso,
			@Value("${cundiapp.jwt.duracion-refresco}") Duration vigenciaRefresco) {
		return new AbridorDeSesion(sesiones, tokens, vigenciaAcceso, vigenciaRefresco);
	}

	@Bean
	IniciarSesion iniciarSesion(
			EstudianteRepositorio estudiantes,
			CifradorDeContrasenaPort cifrador,
			LimitadorDeIntentosPort limitador,
			AbridorDeSesion abridor,
			RelojPort reloj) {
		return new IniciarSesionServicio(estudiantes, cifrador, limitador, abridor, reloj);
	}

	@Bean
	IniciarSesionConGoogle iniciarSesionConGoogle(
			VerificadorDeIdentidadExternaPort verificador,
			VinculoConGoogleRepositorio vinculos,
			EstudianteRepositorio estudiantes,
			AbridorDeSesion abridor,
			RelojPort reloj) {
		return new IniciarSesionConGoogleServicio(verificador, vinculos, estudiantes, abridor, reloj);
	}

	@Bean
	VincularGoogle vincularGoogle(
			EstudianteRepositorio estudiantes,
			VerificadorDeIdentidadExternaPort verificador,
			VinculoConGoogleRepositorio vinculos,
			RelojPort reloj) {
		return new VincularGoogleServicio(estudiantes, verificador, vinculos, reloj);
	}

	@Bean
	DesvincularGoogle desvincularGoogle(VinculoConGoogleRepositorio vinculos) {
		return new DesvincularGoogleServicio(vinculos);
	}

	@Bean
	ConsultarVinculoConGoogle consultarVinculoConGoogle(VinculoConGoogleRepositorio vinculos) {
		return new ConsultarVinculoConGoogleServicio(vinculos);
	}

	@Bean
	RenovarSesion renovarSesion(
			EstudianteRepositorio estudiantes,
			SesionRepositorio sesiones,
			EmisorDeTokensPort tokens,
			RelojPort reloj,
			@Value("${cundiapp.jwt.duracion-acceso}") Duration vigenciaAcceso,
			@Value("${cundiapp.jwt.duracion-refresco}") Duration vigenciaRefresco) {
		return new RenovarSesionServicio(estudiantes, sesiones, tokens, reloj, vigenciaAcceso, vigenciaRefresco);
	}

	@Bean
	CerrarSesion cerrarSesion(SesionRepositorio sesiones, RelojPort reloj) {
		return new CerrarSesionServicio(sesiones, reloj);
	}

	@Bean
	ConsultarMiCuenta consultarMiCuenta(EstudianteRepositorio estudiantes) {
		return new ConsultarMiCuentaServicio(estudiantes);
	}

	@Bean
	EliminarCuenta eliminarCuenta(EstudianteRepositorio estudiantes, SesionRepositorio sesiones, RelojPort reloj) {
		return new EliminarCuentaServicio(estudiantes, sesiones, reloj);
	}

	@Bean
	ReenviarCodigoDeVerificacion reenviarCodigoDeVerificacion(
			EstudianteRepositorio estudiantes,
			CodigoDeVerificacionRepositorio codigos,
			EmisorDeCodigoDeVerificacion emisor,
			RelojPort reloj) {
		return new ReenviarCodigoServicio(estudiantes, codigos, emisor, reloj);
	}
}
