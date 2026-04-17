package com.vedant.concert_platform.config;

import com.vedant.concert_platform.entity.Producer;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.Role;
import com.vedant.concert_platform.repository.ProducerRepository;
import com.vedant.concert_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProducerBootstrapRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final ProducerRepository producerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.producer.email:producer@concert.local}")
    private String producerEmail;

    @Value("${app.bootstrap.producer.password:}")
    private String producerPassword;

    @Value("${app.bootstrap.producer.first-name:Super}")
    private String producerFirstName;

    @Value("${app.bootstrap.producer.last-name:Admin}")
    private String producerLastName;

    @Override
    public void run(String... args) {
        if (producerRepository.count() > 0) {
            return;
        }
        if (producerPassword == null || producerPassword.isBlank()) {
            throw new IllegalStateException("app.bootstrap.producer.password must be configured");
        }

        User user = userRepository.findByEmail(producerEmail).orElseGet(() -> {
            User created = new User();
            created.setEmail(producerEmail);
            created.setPassword(passwordEncoder.encode(producerPassword));
            created.setFirstName(producerFirstName);
            created.setLastName(producerLastName);
            created.setRole(Role.PRODUCER);
            created.setFirstLogin(false);
            created.setMfaEnabled(false);
            return userRepository.save(created);
        });

        Producer producer = new Producer();
        producer.setUser(user);
        producerRepository.save(producer);
    }
}
