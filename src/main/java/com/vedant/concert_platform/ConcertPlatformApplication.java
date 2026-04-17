package com.vedant.concert_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ConcertPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConcertPlatformApplication.class, args);
	}

}
