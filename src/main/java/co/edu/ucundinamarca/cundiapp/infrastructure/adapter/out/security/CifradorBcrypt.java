package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import co.edu.ucundinamarca.cundiapp.application.port.out.CifradorDeContrasenaPort;
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

	@Override
	public boolean coincide(String contrasenaSinCifrar, String contrasenaCifrada) {
		return codificador.matches(contrasenaSinCifrar, contrasenaCifrada);
	}
}
