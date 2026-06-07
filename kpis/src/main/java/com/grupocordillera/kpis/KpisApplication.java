package com.grupocordillera.kpis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class KpisApplication {

	public static void main(String[] args) {
		SpringApplication.run(KpisApplication.class, args);
	}

}
