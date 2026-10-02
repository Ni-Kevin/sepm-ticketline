package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

public class UserRegisterDto {
    private String firstName;

    private String lastName;

    private String email;

    private String password;

    public UserRegisterDto(String userRegisterEmail, String userRegisterPassword, String userRegisterFirstName, String userRegisterLastName) {
        this.firstName = userRegisterFirstName;
        this.lastName = userRegisterLastName;
        this.email = userRegisterEmail;
        this.password = userRegisterPassword;
    }

    public UserRegisterDto() {}

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


    public static final class UserRegisterDtoBuilder {
        private String firstName;
        private String lastName;
        private String email;
        private String password;

        private UserRegisterDtoBuilder() {}

        public static UserRegisterDtoBuilder anUserRegisterDto() {
            return new UserRegisterDtoBuilder();
        }

        public UserRegisterDtoBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserRegisterDtoBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserRegisterDtoBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserRegisterDtoBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public UserRegisterDto build() {
            UserRegisterDto dto = new UserRegisterDto();
            dto.setFirstName(firstName);
            dto.setLastName(lastName);
            dto.setEmail(email);
            dto.setPassword(password);
            return dto;
        }
    }

}
