package com.example.minidrive.configuration;

import com.example.minidrive.configuration.properties.AdminBootstrapProperties;
import com.example.minidrive.entity.User;
import com.example.minidrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AdminBootstrapProperties properties;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.enabled()) {
            return;
        }

        if (userRepository.existsByUsername(properties.username())) {
            return;
        }

        User admin = User.builder()
                .username(properties.username())
                .email(properties.email())
                .emailVerifiedAt(clock.instant())
                .passwordHash(passwordEncoder.encode(properties.password()))
                .quotaBytes(Long.MAX_VALUE)
                .isAdmin(true)
                .build();

        userRepository.save(admin);
    }
}
