package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoNoEnviadoException;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envía el código de verificación al correo institucional por SMTP (CORREO_SMTP_*). El mensaje lleva
 * una versión HTML con los colores de la app y otra en texto plano para los lectores que no muestran HTML.
 */
@Component
@Conditional(ServidorDeCorreo.Configurado.class)
class EnviadorDeCodigoPorCorreo implements EnviadorDeCodigoPort {

	private static final Logger log = LoggerFactory.getLogger(EnviadorDeCodigoPorCorreo.class);
	private static final String NOMBRE_REMITENTE = "CundiApp";
	private static final long MINUTOS_DE_VIGENCIA = CodigoDeVerificacion.VIGENCIA.toMinutes();

	private final JavaMailSender servidor;
	private final String remitente;

	EnviadorDeCodigoPorCorreo(JavaMailSender servidor, @Value("${cundiapp.correo.remitente}") String remitente) {
		this.servidor = servidor;
		this.remitente = remitente;
	}

	@Override
	public void enviar(CorreoInstitucional destino, String codigo) {
		try {
			MimeMessage mensaje = servidor.createMimeMessage();
			var contenido = new MimeMessageHelper(mensaje, true, "UTF-8");
			contenido.setFrom(remitente, NOMBRE_REMITENTE);
			contenido.setTo(destino.valor());
			contenido.setSubject(codigo + " es tu código de verificación de CundiApp");
			contenido.setText(textoPlano(codigo), html(codigo));
			servidor.send(mensaje);
			log.info("Código de verificación enviado por correo");
		} catch (MailAuthenticationException e) {
			log.error("El servidor de correo rechazó el usuario o la clave: revisa CORREO_SMTP_USUARIO y CORREO_SMTP_CLAVE");
			throw sinEnviar(e);
		} catch (MailException | MessagingException | UnsupportedEncodingException e) {
			// Solo el tipo de falla: el mensaje de la excepción puede traer la dirección del destinatario.
			log.error("No se pudo enviar el código de verificación por correo ({})", e.getClass().getSimpleName());
			throw sinEnviar(e);
		}
	}

	private static CorreoNoEnviadoException sinEnviar(Exception causa) {
		return new CorreoNoEnviadoException("No pudimos enviar el código a tu correo. Intenta de nuevo en unos minutos", causa);
	}

	static String textoPlano(String codigo) {
		return """
				Tu código de verificación de CundiApp es: %s

				Escríbelo en la app para activar tu cuenta. Vence en %d minutos.

				Si no creaste una cuenta en CundiApp, ignora este correo: nadie podrá usarla sin este código.
				""".formatted(codigo, MINUTOS_DE_VIGENCIA);
	}

	static String html(String codigo) {
		return """
				<!DOCTYPE html>
				<html lang="es">
				<body style="margin:0;padding:0;background:#f7f8fa;font-family:Arial,Helvetica,sans-serif;color:#374151;">
				  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f7f8fa;padding:24px 12px;">
				    <tr><td align="center">
				      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:480px;background:#ffffff;border:1px solid #e3e6ea;border-radius:16px;">
				        <tr><td style="padding:28px 28px 8px;">
				          <p style="margin:0;font-size:20px;font-weight:bold;color:#0a7a55;">CundiApp</p>
				          <h1 style="margin:16px 0 8px;font-size:22px;color:#111827;">Verifica tu correo institucional</h1>
				          <p style="margin:0;font-size:15px;line-height:1.5;">Escribe este código en la app para activar tu cuenta:</p>
				        </td></tr>
				        <tr><td align="center" style="padding:20px 28px;">
				          <p style="margin:0;padding:16px 20px;background:#e6f7ef;border-radius:12px;font-size:32px;font-weight:bold;letter-spacing:8px;color:#086447;">%s</p>
				        </td></tr>
				        <tr><td style="padding:0 28px 28px;font-size:14px;line-height:1.5;">
				          <p style="margin:0 0 12px;">El código vence en %d minutos.</p>
				          <p style="margin:0;color:#6b7280;">Si no creaste una cuenta en CundiApp, ignora este correo: nadie podrá usarla sin este código.</p>
				        </td></tr>
				      </table>
				      <p style="margin:16px 0 0;font-size:12px;color:#6b7280;">Universidad de Cundinamarca · Proyecto integrador</p>
				    </td></tr>
				  </table>
				</body>
				</html>
				""".formatted(codigo, MINUTOS_DE_VIGENCIA);
	}
}
