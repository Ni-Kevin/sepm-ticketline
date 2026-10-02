package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserUpdateDto {
    @NotBlank(message = "First name is required")
    @Size(max = 30, message = "First name must not exceed 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "First name must not contain numbers or special characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 30, message = "Last name must not exceed 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Last name must not contain numbers or special characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid format")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
    private String password;
    private String oldPassword;
    private String passwordConfirmation;

    public UserUpdateDto() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }


    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public static final class UserUpdateDtoBuilder {
        private String firstName;
        private String lastName;
        private String email;
        private String password;

        public static UserUpdateDtoBuilder anUserUpdateDto() {
            return new UserUpdateDtoBuilder();
        }

        private UserUpdateDtoBuilder() {
        }

        public UserUpdateDtoBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserUpdateDtoBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserUpdateDtoBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserUpdateDtoBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public UserUpdateDto build() {
            UserUpdateDto dto = new UserUpdateDto();
            dto.setFirstName(firstName);
            dto.setLastName(lastName);
            dto.setEmail(email);
            dto.setPassword(password);
            return dto;
        }


    }
}
