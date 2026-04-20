package com.vedant.concert_platform;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
public class ConcertPlatformApplication {

	@Autowired
	private JavaMailSender mailSender;

	public static void main(String[] args) {
		SpringApplication.run(ConcertPlatformApplication.class, args);
	}

}