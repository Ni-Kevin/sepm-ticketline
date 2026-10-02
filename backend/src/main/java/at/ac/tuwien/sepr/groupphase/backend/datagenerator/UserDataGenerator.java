package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;

/**
 * Generates initial user data upon application startup if the database is empty.
 * This ensures that an admin account always exists for E2E tests.
 */
@Component
public class UserDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Constants for the default admin, matching the Cypress settings.json
    private static final String ADMIN_FIRSTNAME = "admin";
    private static final String ADMIN_LASTNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@email.com";
    private static final String ADMIN_PASSWORD = "password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataGenerator(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Executed automatically after dependency injection.
     * Checks the database for existing users to prevent duplicates and unique constraint violations.
     */
    @PostConstruct
    private void generateUser() {
        if (userRepository.count() > 0) {
            LOGGER.debug("users already generated");
        } else {
            LOGGER.debug("generating admin and standard user entries");

            ApplicationUser admin = ApplicationUser.ApplicationUserBuilder.anApplicationUser()
                .withEmail(ADMIN_EMAIL)
                .withPassword(passwordEncoder.encode(ADMIN_PASSWORD))
                .withFirstName(ADMIN_FIRSTNAME)
                .withLastName(ADMIN_LASTNAME)
                .withRole(UserRole.ROLE_ADMIN)
                .withLocked(false)
                .build();

            LOGGER.debug("saving admin user {}", admin);
            userRepository.save(admin);
        }
    }
}
