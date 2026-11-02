package com.research.gbjournal.config;

import com.research.gbjournal.entity.User;
import com.research.gbjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    /** Optional bootstrap password for the seeded admin accounts (INITIAL_ADMIN_PASSWORD). */
    @Value("${INITIAL_ADMIN_PASSWORD:}")
    private String initialAdminPassword;

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

        String rawPassword = initialAdminPassword;
        if (rawPassword == null || rawPassword.length() < 12) {
            byte[] bytes = new byte[18];
            new java.security.SecureRandom().nextBytes(bytes);
            rawPassword = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            // Printed once, only when the user table is empty (first boot). Change it after signing in.
            log.warn("Seeded admin accounts with a generated one-time password: {} "
                    + "(set INITIAL_ADMIN_PASSWORD to choose your own)", rawPassword);
        }
        String encoded = passwordEncoder.encode(rawPassword);

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
