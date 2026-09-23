package co.edu.ucundinamarca.cundiapp.infraestructura.salida.seguridad;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CifradorDeContrasenaPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class CifradorBcrypt implements CifradorDeContrasenaPort {

	private final PasswordEncoder codificador;

	CifradorBcrypt(PasswordEncoder codificador) {
		this.codificador = codificador;
	}

	@Override
	public String cifrar(String contrasenaSinCifrar) {
		return codificador.encode(contrasenaSinCifrar);
	}
}
