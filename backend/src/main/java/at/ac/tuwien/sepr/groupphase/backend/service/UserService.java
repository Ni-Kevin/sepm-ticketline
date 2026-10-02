package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

public interface UserService extends UserDetailsService {

    /**
     * Find a user in the context of Spring Security based on the email address.
     * <br>
     * For more information have a look at this tutorial:
     * https://www.baeldung.com/spring-security-authentication-with-a-database
     *
     * @param email the email address
     * @return a Spring Security user
     * @throws UsernameNotFoundException is thrown if the specified user does not exists
     */
    @Override
    UserDetails loadUserByUsername(String email) throws UsernameNotFoundException;

    /**
     * Find an application user based on the email address.
     *
     * @param email the email address
     * @return a application user
     */
    ApplicationUser findApplicationUserByEmail(String email);

    /**
     * Log in a user.
     *
     * @param userLoginDto login credentials
     * @return the JWT, if successful
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are bad
     */
    String login(UserLoginDto userLoginDto);

    /**
     * Register a new user in the system.
     *
     * @param userRegisterDto the data for the new user
     * @throws ValidationException if the provided data is invalid or the email is already taken
     */
    void register(UserRegisterDto userRegisterDto) throws ValidationException;

    /**
     * Updates the profile data of an existing user.
     * If the email or password is changed, a new authentication token is generated
     * to keep the user session active.
     *
     * @param currentEmail  the email address of the user currently logged in
     * @param userUpdateDto the data transfer object containing the updated user information
     * @return a new JWT authentication token as a {@link String}
     * @throws ValidationException if the new data (email, password, etc.) violates business rules
     * @throws NotFoundException   if no user with the given currentEmail exists
     */
    String update(String currentEmail, UserUpdateDto userUpdateDto) throws ValidationException, NotFoundException;


    /**
     * Deletes the user with all their data from the database.
     *
     * @param emailToDelete the unique email of the user to be deleted
     * @throws NotFoundException if no userwith such an email exists
     */
    void deleteByEmail(String emailToDelete) throws NotFoundException;

    /**
     * Create a new user (or admin) in the system (as an admin).
     *
     * @param userCreateDto the data fpr the new user
     * @throws ValidationException if the provided data is invalid or the email is already taken
     */
    void createUser(UserCreateDto userCreateDto) throws ValidationException;

    /**
     * Updates the locked status of a user.
     *
     * @param id     the ID of the user to update
     * @param locked true to lock the user, false to unlock
     * @throws NotFoundException   if no user with the given ID exists
     * @throws ValidationException if the admin is trying to lock its own account
     */
    void setLockedStatus(Long id, Boolean locked) throws NotFoundException, ValidationException;

    /**
     * Lists all users.
     *
     * @return List of objects of type {@code ApplicationUser} that contains all user data
     */
    List<ApplicationUser> findAll();
}
