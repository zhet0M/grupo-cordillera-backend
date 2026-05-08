package com.grupocordillera.finanzas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FinanzasApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanzasApplication.class, args);
	}

}
