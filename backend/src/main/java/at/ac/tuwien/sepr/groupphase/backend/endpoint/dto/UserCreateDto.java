package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;

import java.util.UUID;

public class UserCreateDto {
    private String firstName;

    private String lastName;

    private String email;

    private UserRole role;

    public UserCreateDto(String userCreateEmail, String userCreateFirstName, String userCreateLastName, UserRole role) {
        this.firstName = userCreateFirstName;
        this.lastName = userCreateLastName;
        this.email = userCreateEmail;
        this.role = role;
    }

    public UserCreateDto() {
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public static final class UserCreateDtoBuilder {
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private UserRole role;

        private UserCreateDtoBuilder() {
        }

        public static UserCreateDtoBuilder anUserCreateDto() {
            return new UserCreateDtoBuilder();
        }

        public UserCreateDtoBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserCreateDtoBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserCreateDtoBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserCreateDtoBuilder withRole(UserRole role) {
            this.role = role;
            return this;
        }

        public UserCreateDto build() {
            UserCreateDto dto = new UserCreateDto();
            dto.setFirstName(firstName);
            dto.setLastName(lastName);
            dto.setEmail(email);
            dto.setRole(role);
            return dto;
        }
    }

}
