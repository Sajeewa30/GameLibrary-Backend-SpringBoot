package com.backend.gamelibrarybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;


@SpringBootApplication
@EnableCaching
public class GamelibrarybackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamelibrarybackendApplication.class, args);
	}

}
