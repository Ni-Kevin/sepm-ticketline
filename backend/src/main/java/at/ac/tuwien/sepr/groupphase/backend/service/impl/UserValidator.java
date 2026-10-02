package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final UserRepository userRepository;
    private static final String EMAIL_PATTERN = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    public UserValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public void validateForRegistration(UserRegisterDto user) throws ValidationException {
        LOGGER.trace("validateForRegistration({})", user);
        List<String> validationErrors = new ArrayList<>();

        validateFirstName(user.getFirstName(), validationErrors);
        validateLastName(user.getLastName(), validationErrors);
        validateEmail(user.getEmail(), validationErrors);

        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            validationErrors.add("email: An account with this email address already exists");
        }

        validatePasswordStrength(user.getPassword(), validationErrors);

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Validation failed", validationErrors);
        }
    }

    public void validateForCreation(UserCreateDto user) throws ValidationException {
        LOGGER.trace("validateForCreation({})", user);
        List<String> validationErrors = new ArrayList<>();

        validateFirstName(user.getFirstName(), validationErrors);
        validateLastName(user.getLastName(), validationErrors);
        validateEmail(user.getEmail(), validationErrors);

        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            validationErrors.add("email: An account with this email address already exists");
        }

        if (user.getRole() == null) {
            validationErrors.add("role: Role must not be null");
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Validation failed", validationErrors);
        }
    }

    private void validateFirstName(String firstName, List<String> errors) {
        if (firstName == null || firstName.isBlank()) {
            errors.add("firstName: First name is required");
            return;
        }
        if (firstName.length() > 30) {
            errors.add("firstName: First name must not exceed 30 characters");
        }
        if (!firstName.matches("^[a-zA-Z\\s]*$")) {
            errors.add("firstName: First name must not contain numbers or special characters");
        }
    }

    private void validateLastName(String lastName, List<String> errors) {
        if (lastName == null || lastName.isBlank()) {
            errors.add("lastName: Last name is required");
            return;
        }
        if (lastName.length() > 30) {
            errors.add("lastName: Last name must not exceed 30 characters");
        }
        if (!lastName.matches("^[a-zA-Z\\s]*$")) {
            errors.add("lastName: Last name must not contain numbers or special characters");
        }
    }

    private void validateEmail(String email, List<String> errors) throws ValidationException {
        LOGGER.trace("validateEmail({})", email);
        if (email == null || email.isBlank()) {
            errors.add("email: Email is required");
            return;
        }
        if (!email.matches(EMAIL_PATTERN)) {
            errors.add("email: Invalid email address");
        }
    }


    /**
     * Checks if the password meets all complexity requirements.
     */
    private void validatePasswordStrength(String password, List<String> errors) {
        String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";

        if (!password.matches(passwordRegex)) {
            errors.add("password: Password must contain at least: 8 Characters, 1 Uppercase, 1 Lowercase, 1 Number, 1 Special Character!");
        }
    }

    public void validatePassword(String password) throws ValidationException {
        List<String> validationErrors = new ArrayList<>();
        if (password == null || password.isBlank()) {
            validationErrors.add("password: Password is required");
        }
        validatePasswordStrength(password, validationErrors);

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Validation failed", validationErrors);
        }
    }

    /**
     * Validate the data to be updated with.
     *
     * @param currentEmail  current email of the user
     * @param userUpdateDto new data
     * @throws ValidationException if new data is invalid
     */
    public void validateForUpdate(String currentEmail, UserUpdateDto userUpdateDto) throws ValidationException {
        List<String> validationErrors = new ArrayList<>();

        if (!currentEmail.equals(userUpdateDto.getEmail())) {
            if (userRepository.findByEmail(userUpdateDto.getEmail()).isPresent()) {
                validationErrors.add("email: Email " + userUpdateDto.getEmail() + " is already taken by another user.");
            }
        }

        validateEmail(userUpdateDto.getEmail(), validationErrors);

        if (userUpdateDto.getPassword() != null && !userUpdateDto.getPassword().isBlank()) {

            if (!userUpdateDto.getPassword().equals(userUpdateDto.getPasswordConfirmation())) {
                validationErrors.add("password: New password and confirmation do not match!");
            }

            validatePasswordStrength(userUpdateDto.getPassword(), validationErrors);
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Validation failed for user update", validationErrors);
        }
    }
}
