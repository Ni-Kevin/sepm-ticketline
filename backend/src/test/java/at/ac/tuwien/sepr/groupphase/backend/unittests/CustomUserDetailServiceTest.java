package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.UserMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.CustomUserDetailService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CustomUserDetailService}.
 * These tests use Mockito to isolate the service layer from its dependencies
 * to test the registration and login logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class CustomUserDetailServiceTest implements TestData {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidator validator;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtTokenizer jwtTokenizer;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CustomUserDetailService userService;

    private ApplicationUser testUser;

    @BeforeEach
    public void beforeEach() {
        testUser = ApplicationUser.ApplicationUserBuilder.anApplicationUser()
            .withFirstName(USER_AUTH_FIRST_NAME)
            .withLastName(USER_AUTH_LAST_NAME)
            .withEmail(USER_AUTH_EMAIL)
            .withPassword("encodedPassword")
            .withRole(UserRole.ROLE_USER)
            .withLocked(false)
            .withFailedLoginAttempts(0)
            .build();
    }

    /**
     * Tests successful user registration.
     * Verifies that the validator is called, the password is encoded, and the user is saved.
     *
     * @throws ValidationException if the validation fails
     */
    @Test
    public void givenNothing_whenRegister_thenPasswordIsEncodedAndUserSaved() throws ValidationException {
        UserRegisterDto registerDto = new UserRegisterDto(USER_REGISTER_EMAIL, USER_REGISTER_PASSWORD, USER_REGISTER_FIRST_NAME, USER_REGISTER_LAST_NAME);
        ApplicationUser mappedUser = new ApplicationUser();
        mappedUser.setEmail(USER_REGISTER_EMAIL);

        when(userMapper.userRegisterDtoToApplicationUser(registerDto)).thenReturn(mappedUser);
        when(passwordEncoder.encode(USER_REGISTER_PASSWORD)).thenReturn("encodedPassword");
        when(userRepository.save(any(ApplicationUser.class))).thenReturn(mappedUser);

        userService.register(registerDto);

        assertAll(
            () -> verify(validator).validateForRegistration(registerDto),
            () -> verify(passwordEncoder).encode(USER_REGISTER_PASSWORD),
            () -> verify(userRepository).save(argThat(user -> user.getPassword().equals("encodedPassword")))
        );
    }

    /**
     * Tests registration failure due to validation errors.
     * Verifies that a ValidationException is thrown and the repository save method is never called.
     */
    @Test
    public void givenNothing_whenRegister_thenValidationExceptionIsThrown() throws ValidationException {
        UserRegisterDto registerDto = new UserRegisterDto("wrong", "123", "", "");

        doThrow(new ValidationException("Invalid data", null)).when(validator).validateForRegistration(registerDto);

        assertThrows(ValidationException.class, () -> userService.register(registerDto));
        verify(userRepository, never()).save(any());
    }

    /**
     * Tests login with valid credentials.
     * Verifies that failed login attempts are reset to 0 and a valid token is returned.
     */
    @Test
    public void loginWithValidCredentials_ShouldResetAttemptsAndReturnToken() {
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_PASSWORD)
            .build();

        testUser.setFailedLoginAttempts(3);

        when(userRepository.findByEmail(USER_AUTH_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(USER_AUTH_PASSWORD, "encodedPassword")).thenReturn(true);
        when(jwtTokenizer.getAuthToken(any(), any())).thenReturn("fake-token");

        String token = userService.login(loginDto);

        assertAll(
            () -> assertEquals("fake-token", token),
            () -> assertEquals(0, testUser.getFailedLoginAttempts()),
            () -> verify(userRepository).save(testUser)
        );
    }

    /**
     * Tests login with an invalid password.
     * Verifies that the failed login attempts counter is incremented and saved.
     */
    @Test
    public void loginWithInvalidPassword_ShouldIncreaseAttempts() {
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_WRONG_PASSWORD)
            .build();

        when(userRepository.findByEmail(USER_AUTH_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(USER_AUTH_WRONG_PASSWORD, "encodedPassword")).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> userService.login(loginDto));

        assertAll(
            () -> assertTrue(exception.getMessage().contains("Invalid email or password")),
            () -> assertEquals(1, testUser.getFailedLoginAttempts()),
            () -> verify(userRepository).save(testUser),
            () -> assertFalse(testUser.getLocked())
        );
    }

    /**
     * Tests account locking after five failed attempts for regular users.
     * Verifies that the user status is set to locked in the database.
     */
    @Test
    public void loginAttemptFive_ShouldLockUser() {
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_WRONG_PASSWORD)
            .build();

        testUser.setFailedLoginAttempts(4);

        when(userRepository.findByEmail(USER_AUTH_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(USER_AUTH_WRONG_PASSWORD, "encodedPassword")).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> userService.login(loginDto));

        assertAll(
            () -> assertTrue(exception.getMessage().contains("Invalid email or password")),
            () -> assertEquals(5, testUser.getFailedLoginAttempts()),
            () -> assertTrue(testUser.getLocked()),
            () -> verify(userRepository).save(testUser)
        );
    }

    /**
     * Tests that administrators are not locked even after five failed attempts.
     * Verifies that the failed attempts counter increases but the locked status remains false.
     */
    @Test
    public void adminLoginFail_ShouldNotLockAdmin() {
        testUser.setRole(UserRole.ROLE_ADMIN);
        testUser.setFailedLoginAttempts(4);

        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(ADMIN_USER)
            .withPassword(USER_AUTH_WRONG_PASSWORD)
            .build();

        when(userRepository.findByEmail(ADMIN_USER)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(USER_AUTH_WRONG_PASSWORD, "encodedPassword")).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> userService.login(loginDto));

        assertAll(
            () -> assertEquals("Invalid email or password.", exception.getMessage()),
            () -> assertEquals(5, testUser.getFailedLoginAttempts()),
            () -> assertFalse(testUser.getLocked(), "Admin should never be locked")
        );
    }

    /**
     * Tests login attempt for an already locked account.
     * Verifies that a BadCredentialsException is thrown immediately without checking the password.
     */
    @Test
    public void loginWhenAlreadyLocked_ShouldThrowImmediateException() {
        testUser.setLocked(true);
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_PASSWORD)
            .build();

        when(userRepository.findByEmail(USER_AUTH_EMAIL)).thenReturn(Optional.of(testUser));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> userService.login(loginDto));
        assertEquals("Invalid email or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }


    /**
     * Tests if the userdata is updated correct if the user leaves the password empty so he contiues with their old one.
     *
     * @throws ValidationException if the userdata is invalid
     */
    @Test
    public void givenValidUpdateDataNoPassword_whenUpdate_thenDataIsSaved() throws ValidationException {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("New");
        updateDto.setLastName("Name");
        updateDto.setEmail(USER_AUTH_EMAIL);
        updateDto.setPassword("");

        when(userRepository.findByEmail(USER_AUTH_EMAIL)).thenReturn(Optional.of(testUser));

        userService.update(USER_AUTH_EMAIL, updateDto);

        assertAll(
            () -> verify(validator).validateForUpdate(USER_AUTH_EMAIL, updateDto),
            () -> verify(userRepository).save(argThat(user ->
                user.getFirstName().equals("New") &&
                    user.getLastName().equals("Name") &&
                    user.getPassword().equals("encodedPassword")
            )),
            () -> verify(passwordEncoder, never()).encode(any())
        );
    }

    /**
     * Tests if the userdata is updated correct if the user filles in a new  password.
     *
     * @throws ValidationException if the userdata is invalid
     */
    @Test
    public void givenValidUpdateWithPassword_whenUpdate_thenPasswordIsEncoded() throws ValidationException {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setEmail(USER_AUTH_EMAIL);
        updateDto.setOldPassword("OldPassword1!");
        updateDto.setPassword("NewSecurePass1!");

        when(userRepository.findByEmail(USER_AUTH_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword1!", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewSecurePass1!")).thenReturn("newEncodedHash");

        userService.update(USER_AUTH_EMAIL, updateDto);

        assertAll(
            () -> verify(passwordEncoder).encode("NewSecurePass1!"),
            () -> verify(userRepository).save(argThat(user -> user.getPassword().equals("newEncodedHash")))
        );
    }

    /**
     * Try to edit a profile that does not exist, should throw Not Found
     * and do not save any new data
     */
    @Test
    public void givenNonExistingEmail_whenUpdate_thenThrowNotFoundException() {
        UserUpdateDto updateDto = new UserUpdateDto();
        String unknownEmail = "ghost@test.at";

        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            userService.update(unknownEmail, updateDto)
        );

        verify(userRepository, never()).save(any());
    }

    /**
     * Tests successful user creation with valid data.
     * Verifies that the user is saved in the repository when the email does not already exist.
     *
     * @throws ValidationException if the validation fails
     */
    @Test
    public void givenValidData_whenCreateUser_thenUserIsSaved() throws ValidationException {
        UserCreateDto dto = new UserCreateDto();
        dto.setFirstName(ADMIN_CREATE_FIRST_NAME);
        dto.setLastName(ADMIN_CREATE_LAST_NAME);
        dto.setEmail(ADMIN_CREATE_EMAIL);
        dto.setRole(UserRole.ROLE_USER);

        ApplicationUser mockUser = new ApplicationUser();
        when(userMapper.userCreateDtoToApplicationUser(any())).thenReturn(mockUser);

        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(dto);

        verify(userRepository, times(1)).save(any());
    }

    /**
     * Tests user creation failure when the email already exists.
     * Verifies that a ValidationException is thrown and the user is not saved when the email is already in use.
     *
     * @throws ValidationException if the validation fails
     */
    @Test
    public void givenExistingEmail_whenCreateUser_thenValidationExceptionAndNoSave() throws ValidationException {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail(USER_AUTH_EMAIL);
        dto.setRole(UserRole.ROLE_USER);

        doThrow(new ValidationException("Validation failed", List.of("Email already exists")))
            .when(validator).validateForCreation(any());

        assertThrows(ValidationException.class, () -> userService.createUser(dto));
        verify(userRepository, never()).save(any());
    }
}