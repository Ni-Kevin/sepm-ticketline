package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository tests for {@link UserRepository}.
 * Uses {@link DataJpaTest} to provide an isolated environment for testing
 * persistence logic and custom query methods against an in-memory database.
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest implements TestData {

    @Autowired
    private UserRepository userRepository;

    /**
     * Resets the database state before each test case.
     * Ensures that data from previous tests does not affect the current execution.
     */
    @BeforeEach
    public void beforeEach() {
        userRepository.deleteAll();
    }

    /**
     * Tests saving a user and retrieving it by email.
     * Verifies that the persistence layer correctly stores the user and
     * that both findByEmail and existsByEmail function as expected for existing data.
     */
    @Test
    public void givenNothing_whenSaveUser_thenFindUserByEmailAndExistsByEmailTrue() {
        ApplicationUser user = new ApplicationUser();
        user.setEmail(USER_REGISTER_EMAIL);
        user.setFirstName(USER_REGISTER_FIRST_NAME);
        user.setLastName(USER_REGISTER_LAST_NAME);
        user.setPassword(USER_REGISTER_PASSWORD);
        user.setRole(UserRole.ROLE_USER);

        userRepository.save(user);

        Optional<ApplicationUser> foundUser = userRepository.findByEmail(USER_REGISTER_EMAIL);
        boolean exists = userRepository.existsByEmail(USER_REGISTER_EMAIL);

        assertAll(
            () -> assertTrue(foundUser.isPresent()),
            () -> assertEquals(USER_REGISTER_EMAIL, foundUser.get().getEmail()),
            () -> assertTrue(exists)
        );
    }

    /**
     * Tests the behavior of query methods for non-existent email addresses.
     * Verifies that the repository correctly returns empty results and false
     * status when searching for records that have not been persisted.
     */
    @Test
    public void givenNothing_whenCheckNonExistingEmail_thenExistsByEmailFalseAndOptionalEmpty() {
        boolean exists = userRepository.existsByEmail("non-existent@email.com");
        Optional<ApplicationUser> foundUser = userRepository.findByEmail("non-existent@email.com");

        assertAll(
            () -> assertFalse(exists),
            () -> assertTrue(foundUser.isEmpty())
        );
    }
}