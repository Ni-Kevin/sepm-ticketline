package at.ac.tuwien.sepr.groupphase.backend.entity;

import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//TODO: replace this class with a correct ApplicationUser Entity implementation
@Entity
@Table(name = "application_user")
public class ApplicationUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private UserRole role = UserRole.ROLE_USER;

    @Column(nullable = false)
    private Boolean locked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<NewsReadState> readStates = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PasswordResetToken> passwordResetTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Order> purchaseOrders = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<SeatHold> seatHolds = new ArrayList<>();

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public List<PasswordResetToken> getPasswordResetTokens() {
        return passwordResetTokens;
    }

    public void setPasswordResetTokens(List<PasswordResetToken> passwordResetTokens) {
        this.passwordResetTokens = passwordResetTokens;
    }

    public List<NewsReadState> getReadStates() {
        return readStates;
    }

    public void setReadStates(List<NewsReadState> readStates) {
        this.readStates = readStates;
    }

    public List<SeatHold> getSeatHolds() {
        return seatHolds;
    }

    public void setSeatHolds(List<SeatHold> seatHolds) {
        this.seatHolds = seatHolds;
    }

    public List<Order> getPurchaseOrders() {
        return purchaseOrders;
    }

    public void setPurchaseOrders(List<Order> purchaseOrders) {
        this.purchaseOrders = purchaseOrders;
    }

    public ApplicationUser() {
    }

    public ApplicationUser(String firstName, String lastName, String email, String password, UserRole role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.locked = false;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public UserRole getRole() {

        return role;
    }

    public void setRole(UserRole role) {

        this.role = role;
    }

    public Boolean getLocked() {

        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationUser that)) {
            return false;
        }
        return Objects.equals(id, that.id) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "ApplicationUser{"
            + "id=" + id
            + ", firstName='" + firstName + '\''
            + ", lastName='" + lastName + '\''
            + ", email='" + email + '\''
            + ", admin=" + role
            + ", locked=" + locked
            + '}';
    }


    public static final class ApplicationUserBuilder {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private UserRole role = UserRole.ROLE_USER;
        private Boolean locked = false;
        private Integer failedLoginAttempts = 0;

        private ApplicationUserBuilder() {
        }

        public static ApplicationUserBuilder anApplicationUser() {
            return new ApplicationUserBuilder();
        }

        public ApplicationUserBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public ApplicationUserBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public ApplicationUserBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public ApplicationUserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public ApplicationUserBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public ApplicationUserBuilder withRole(UserRole role) {
            this.role = role;
            return this;
        }

        public ApplicationUserBuilder withLocked(Boolean locked) {
            this.locked = locked;
            return this;
        }


        public ApplicationUserBuilder withFailedLoginAttempts(Integer failedLoginAttempts) {
            this.failedLoginAttempts = failedLoginAttempts;
            return this;
        }


        public ApplicationUser build() {
            ApplicationUser user = new ApplicationUser();
            user.setId(id);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(role);
            user.setLocked(locked);
            user.setFailedLoginAttempts(failedLoginAttempts);
            return user;
        }
    }
}
