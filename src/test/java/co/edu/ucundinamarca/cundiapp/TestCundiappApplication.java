package co.edu.ucundinamarca.cundiapp;

import org.springframework.boot.SpringApplication;

public class TestCundiappApplication {

	public static void main(String[] args) {
		SpringApplication.from(CundiappApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
