package com.research.gbjournal.config;

import com.research.gbjournal.entity.User;
import com.research.gbjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "demopass";

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== DataInitializer: Database check ===");
        ensureInitialUsers();
        log.info("=== DataInitializer: Check complete ===");
    }

    /**
     * Ensures baseline administrator & operational roles exist if the database
     * user table is empty. All articles, issues, and submissions are managed
     * purely in the database.
     */
    private void ensureInitialUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        String encoded = passwordEncoder.encode(DEFAULT_PASSWORD);

        List<User> initialUsers = List.of(
                User.builder()
                        .fullName("Prof. Dr. Laila Rahman")
                        .email("superadmin@gonouniversity.edu.bd")
                        .password(encoded)
                        .role(User.Role.SUPER_ADMIN)
                        .title("Editor-in-Chief & Administrator")
                        .department("Faculty of Health Sciences")
                        .institution("Gono Bishwabidyalay")
                        .country("BD")
                        .enabled(true)
                        .emailVerified(true)
                        .build(),

                User.builder()
                        .fullName("Md. Jamil Hossain")
                        .email("admin@gonouniversity.edu.bd")
                        .password(encoded)
                        .role(User.Role.ADMIN)
                        .title("System Administrator")
                        .department("Journal Operations")
                        .institution("Gono Bishwabidyalay")
                        .country("BD")
                        .enabled(true)
                        .emailVerified(true)
                        .build()
        );

        userRepository.saveAll(initialUsers);
        log.info("Initialized default admin users");
    }
}
