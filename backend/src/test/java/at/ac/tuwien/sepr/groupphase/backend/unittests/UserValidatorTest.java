package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserValidatorTest {

    @Mock
    private UserRepository userRepository;

    private UserValidator userValidator;

    @BeforeEach
    public void beforeEach() {
        userValidator = new UserValidator(userRepository);
    }

    @Test
    public void validateForRegistrationWithValidData_succeeds() {
        UserRegisterDto dto = new UserRegisterDto("user@example.com", "StrongP@ss1", "John", "Doe");

        assertDoesNotThrow(() -> userValidator.validateForRegistration(dto));
    }

    @Test
    public void validateForRegistrationWithDuplicateEmail_throwsValidationException() {
        UserRegisterDto dto = new UserRegisterDto("existing@example.com", "StrongP@ss1", "John", "Doe");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(ValidationException.class, () -> userValidator.validateForRegistration(dto));
    }

    @Test
    public void validateForRegistrationWithInvalidData_throwsAllErrors() {
        UserRegisterDto dto = new UserRegisterDto(null, "weak", null, null);

        ValidationException exception = assertThrows(ValidationException.class, () -> userValidator.validateForRegistration(dto));

        assertAll(
            () -> assertTrue(exception.errors().stream().anyMatch(e -> e.contains("firstName"))),
            () -> assertTrue(exception.errors().stream().anyMatch(e -> e.contains("lastName"))),
            () -> assertTrue(exception.errors().stream().anyMatch(e -> e.contains("email"))),
            () -> assertTrue(exception.errors().stream().anyMatch(e -> e.contains("password")))
        );
    }

    @Test
    public void validateForRegistrationWithInvalidEmailFormat_throwsValidationException() {
        UserRegisterDto dto = new UserRegisterDto("not-an-email", "StrongP@ss1", "John", "Doe");

        ValidationException exception = assertThrows(ValidationException.class, () -> userValidator.validateForRegistration(dto));
        assertTrue(exception.errors().stream().anyMatch(e -> e.contains("email")));
    }

    @Test
    public void validateForCreationWithValidData_succeeds() {
        UserCreateDto dto = new UserCreateDto("newuser@example.com", "Jane", "Smith", UserRole.ROLE_USER);

        assertDoesNotThrow(() -> userValidator.validateForCreation(dto));
    }

    @Test
    public void validateForCreationWithNullRole_throwsValidationException() {
        UserCreateDto dto = new UserCreateDto("newuser@example.com", "Jane", "Smith", null);

        ValidationException exception = assertThrows(ValidationException.class, () -> userValidator.validateForCreation(dto));
        assertTrue(exception.errors().stream().anyMatch(e -> e.contains("role")));
    }

    @Test
    public void validateForUpdateWithValidData_succeeds() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("user@example.com");

        assertDoesNotThrow(() -> userValidator.validateForUpdate("user@example.com", dto));
    }

    @Test
    public void validateForUpdateWithEmailAlreadyTaken_throwsValidationException() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("taken@example.com");

        when(userRepository.findByEmail("taken@example.com")).thenReturn(java.util.Optional.of(new at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser()));

        ValidationException exception = assertThrows(ValidationException.class,
            () -> userValidator.validateForUpdate("old@example.com", dto));
        assertTrue(exception.errors().stream().anyMatch(e -> e.contains("email")));
    }

    @Test
    public void validateForUpdateWithPasswordMismatch_throwsValidationException() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("user@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setPassword("NewStr0ng@Pass");
        dto.setPasswordConfirmation("DifferentPass1!");

        ValidationException exception = assertThrows(ValidationException.class,
            () -> userValidator.validateForUpdate("user@example.com", dto));
        assertTrue(exception.errors().stream().anyMatch(e -> e.contains("password")));
    }

    @Test
    public void validateForUpdateWithWeakPassword_throwsValidationException() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("user@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setPassword("weak");
        dto.setPasswordConfirmation("weak");

        ValidationException exception = assertThrows(ValidationException.class,
            () -> userValidator.validateForUpdate("user@example.com", dto));
        assertTrue(exception.errors().stream().anyMatch(e -> e.contains("password")));
    }

    @Test
    public void validateForUpdateWithNullPassword_skipsValidation() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("user@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setPassword(null);

        assertDoesNotThrow(() -> userValidator.validateForUpdate("user@example.com", dto));
    }

}
