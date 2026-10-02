package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PasswordResetConfirmDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PasswordResetRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.PasswordResetToken;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.PasswordResetTokenRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserEndpointTest implements TestData {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ApplicationUser testUser;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private static final String PASSWORD_RESET_REQUEST_URI = BASE_URI + "/users/password-reset/request";
    private static final String PASSWORD_RESET_CONFIRM_URI = BASE_URI + "/users/password-reset/confirm";
    private static final String TEST_RAW_TOKEN = "integration-test-token";
    private static final String NEW_PASSWORD = "NewStr0ng@Pass1";

    // --- REGISTRATION TESTS ---

    /**
     * Resets the database and creates a fresh test user before each test execution.
     */
    @BeforeEach
    public void beforeEach() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        testUser = ApplicationUser.ApplicationUserBuilder.anApplicationUser()
            .withFirstName(USER_AUTH_FIRST_NAME)
            .withLastName(USER_AUTH_LAST_NAME)
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(passwordEncoder.encode(USER_AUTH_PASSWORD))
            .withRole(UserRole.ROLE_USER)
            .withLocked(false)
            .withFailedLoginAttempts(0)
            .build();
        userRepository.save(testUser);
    }

    /**
     * Tests a successful registration flow.
     * Verifies that valid user data results in an HTTP 201 Created status
     * and a response body containing the correct user details.
     *
     * @throws Exception if any error occurs during the mock request
     */
    @Test
    public void givenValidUserData_whenRegister_then201() throws Exception {
        UserRegisterDto userRegisterDto = new UserRegisterDto();
        userRegisterDto.setEmail(USER_REGISTER_EMAIL);
        userRegisterDto.setFirstName(USER_REGISTER_FIRST_NAME);
        userRegisterDto.setLastName(USER_REGISTER_LAST_NAME);
        userRegisterDto.setPassword(USER_REGISTER_PASSWORD);

        String body = jsonMapper.writeValueAsString(userRegisterDto);

        MvcResult mvcResult = this.mockMvc.perform(post(REGISTER_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
    }

    /**
     * Tests registration with invalid data that violates business logic (e.g., weak password).
     * Verifies that the system returns HTTP 422 Unprocessable Entity and a structured error response.
     *
     * @throws Exception if any error occurs during the mock request
     */
    @Test
    public void givenInvalidPassword_whenRegister_then422() throws Exception {
        UserRegisterDto userRegisterDto = new UserRegisterDto();
        userRegisterDto.setEmail(USER_REGISTER_EMAIL);
        userRegisterDto.setFirstName(USER_REGISTER_FIRST_NAME);
        userRegisterDto.setLastName(USER_REGISTER_LAST_NAME);
        userRegisterDto.setPassword("Password");

        String body = jsonMapper.writeValueAsString(userRegisterDto);

        MvcResult mvcResult = this.mockMvc.perform(post(REGISTER_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());

        String content = response.getContentAsString();
        assertTrue(content.contains("\"errors\""));
    }

    /**
     * Tests registration with malformed data.
     * Verifies that the system returns HTTP 400 Bad Request.
     *
     * @throws Exception if any error occurs during the mock request
     */
    @Test
    public void givenInvalidEmail_whenRegister_then400() throws Exception {
        UserRegisterDto userRegisterDto = new UserRegisterDto();
        userRegisterDto.setEmail("blabla");
        userRegisterDto.setFirstName(USER_REGISTER_FIRST_NAME);
        userRegisterDto.setLastName(USER_REGISTER_LAST_NAME);
        userRegisterDto.setPassword(USER_REGISTER_PASSWORD);

        String body = jsonMapper.writeValueAsString(userRegisterDto);

        MvcResult mvcResult = this.mockMvc.perform(post(REGISTER_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());

        String content = response.getContentAsString();
        assertTrue(content.contains("\"errors\""));
    }

    // --- LOGIN TESTS ---


    /**
     * Tests successful authentication with valid credentials.
     * Verifies that the server responds with HTTP 200 OK and provides a non-empty JWT token.
     *
     * @throws Exception if MockMvc request execution fails
     */
    @Test
    public void givenCorrectCredentials_whenLogin_then200AndJwtToken() throws Exception {
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_PASSWORD)
            .build();

        MvcResult mvcResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginDto)))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        String token = response.getContentAsString();

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertNotNull(token),
            () -> assertFalse(token.isEmpty())
        );
    }

    /**
     * Tests the sequential failure of login attempts.
     * Verifies that each failure increases the counter and that the 5th attempt
     * results in a locked account status in the database.
     *
     * @throws Exception if MockMvc request execution fails
     */
    @Test
    public void givenWrongPassword_whenLoginRepeatedly_then401AndAccountLockedAndGenericMessage() throws Exception {
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_WRONG_PASSWORD)
            .build();
        String body = jsonMapper.writeValueAsString(loginDto);

        // Perform 4 failed attempts and check "remaining attempts" message
        for (int i = 1; i <= 4; i++) {
            MvcResult mvcResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andDo(print())
                .andReturn();

            MockHttpServletResponse response = mvcResult.getResponse();

            assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus()),
                () -> assertTrue(response.getContentAsString().contains("Invalid email or password"))
            );
        }

        // Perform the 5th attempt which should trigger the lock
        MvcResult mvcResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse finalResponse = mvcResult.getResponse();
        ApplicationUser updatedUser = userRepository.findByEmail(USER_AUTH_EMAIL).orElseThrow();

        assertAll(
            () -> assertEquals(HttpStatus.UNAUTHORIZED.value(), finalResponse.getStatus()),
            () -> assertTrue(finalResponse.getContentAsString().contains("Invalid email or password")),
            () -> assertTrue(updatedUser.getLocked())
        );
    }

    /**
     * Tests that a locked user cannot log in even with correct credentials.
     * Verifies that the endpoint returns a 401 Unauthorized status and the generic error message.
     *
     * @throws Exception if MockMvc request execution fails
     */
    @Test
    public void givenLockedAccount_whenLoginWithCorrectCredentials_then401AndGenericMessage() throws Exception {
        testUser.setLocked(true);
        userRepository.save(testUser);

        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_PASSWORD)
            .build();

        MvcResult mvcResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginDto)))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus()),
            () -> assertEquals("Invalid email or password", response.getContentAsString())
        );
    }

    /**
     * Tests that admin accounts are exempt from locking.
     * Verifies that even after multiple failed attempts, an admin user remains unlocked in the database.
     *
     * @throws Exception if MockMvc request execution fails
     */
    @Test
    public void givenAdminUser_whenLoginFailsMultipleTimes_thenNeverLocked() throws Exception {
        ApplicationUser admin = ApplicationUser.ApplicationUserBuilder.anApplicationUser()
            .withFirstName(USER_AUTH_FIRST_NAME)
            .withLastName(USER_AUTH_LAST_NAME)
            .withEmail(ADMIN_USER)
            .withPassword(passwordEncoder.encode(USER_AUTH_PASSWORD))
            .withRole(UserRole.ROLE_ADMIN)
            .withLocked(false)
            .withFailedLoginAttempts(4)
            .build();
        userRepository.save(admin);

        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(ADMIN_USER)
            .withPassword(USER_AUTH_WRONG_PASSWORD)
            .build();

        MvcResult mvcResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginDto)))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        ApplicationUser updatedAdmin = userRepository.findByEmail(ADMIN_USER).get();

        assertAll(
            () -> assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus()),
            () -> assertFalse(updatedAdmin.getLocked(), "Admins should not be locked")
        );
    }

    // --- PROFILE MANAGEMENT TESTS ---

    /**
     * Tests updating the personal data of the authenticated user.
     */
    @Test
    public void givenLoggedInUser_whenUpdateMe_then200AndDataChangedInDb() throws Exception {
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_PASSWORD)
            .build();

        MvcResult loginResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginDto)))
            .andReturn();
        String token = loginResult.getResponse().getContentAsString();

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("UpdatedFirstName");
        updateDto.setLastName("UpdatedLastName");
        updateDto.setEmail(USER_AUTH_EMAIL);


        MvcResult updateResult = this.mockMvc.perform(MockMvcRequestBuilders.put(BASE_URI + "/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateDto)))
            .andDo(print())
            .andReturn();

        ApplicationUser updatedUser = userRepository.findByEmail(USER_AUTH_EMAIL).orElseThrow();
        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), updateResult.getResponse().getStatus()),
            () -> assertEquals("UpdatedFirstName", updatedUser.getFirstName()),
            () -> assertEquals("UpdatedLastName", updatedUser.getLastName())
        );
    }

    /**
     * Tests deleting the authenticated user's own account.
     */
    @Test
    public void givenLoggedInUser_whenDeleteMe_then204AndUserRemovedFromDb() throws Exception {
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_PASSWORD)
            .build();

        MvcResult loginResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginDto)))
            .andReturn();
        String token = loginResult.getResponse().getContentAsString();

        MvcResult deleteResult = this.mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URI + "/users/me")
                .header("Authorization", "Bearer " + token))
            .andDo(print())
            .andReturn();

        boolean userExists = userRepository.findByEmail(USER_AUTH_EMAIL).isPresent();
        assertAll(
            () -> assertEquals(HttpStatus.NO_CONTENT.value(), deleteResult.getResponse().getStatus()),
            () -> assertFalse(userExists, "User should have been deleted from the database")
        );
    }

    /**
     * Tests updating the user profile with a password that violates the security policy.
     * Verifies that the server returns HTTP 400 (Bad Request).
     */
    @Test
    public void givenLoggedInUser_whenUpdateWithWeakPassword_then400Error() throws Exception {
        UserLoginDto loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withEmail(USER_AUTH_EMAIL)
            .withPassword(USER_AUTH_PASSWORD)
            .build();

        MvcResult loginResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginDto)))
            .andReturn();
        String token = loginResult.getResponse().getContentAsString();

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("Jakob");
        updateDto.setLastName("Rogy");
        updateDto.setEmail(USER_AUTH_EMAIL);
        updateDto.setPassword("Password123"); // Invaild

        MvcResult updateResult = this.mockMvc.perform(MockMvcRequestBuilders.put(BASE_URI + "/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateDto)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.BAD_REQUEST.value(), updateResult.getResponse().getStatus());
    }

    /**
     * Tests accessing the 'me' endpoint without a JWT token.
     * Verifies that Spring Security returns HTTP 403 Forbidden.
     */
    @Test
    public void givenNoToken_whenGetMe_then403Error() throws Exception {
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(BASE_URI + "/users/me")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), result.getResponse().getStatus());
    }

    /**
     * Tests that an admin user can successfully create a new customer.
     * Verifies that the endpoint returns HTTP 201 Created and that the new user is persisted in the database with the correct details.
     *
     * @throws Exception if MockMvc request execution fails
     */
    @Test
    public void givenAdminLoggedIn_whenCreateCustomer_then201AndUserPersistedInDb() throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setFirstName(ADMIN_CREATE_FIRST_NAME);
        dto.setLastName(ADMIN_CREATE_LAST_NAME);
        dto.setEmail(ADMIN_CREATE_EMAIL);
        dto.setRole(UserRole.ROLE_USER);

        MvcResult mvcResult = this.mockMvc.perform(post(ADMIN_CREATE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dto))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.CREATED.value(), mvcResult.getResponse().getStatus());

        ApplicationUser created = userRepository.findByEmail(ADMIN_CREATE_EMAIL).orElseThrow();
        assertAll(
            () -> assertEquals(ADMIN_CREATE_FIRST_NAME, created.getFirstName()),
            () -> assertEquals(ADMIN_CREATE_LAST_NAME, created.getLastName()),
            () -> assertEquals(UserRole.ROLE_USER, created.getRole())
        );
    }

    /**
     * Tests that creating a user with an email that already exists results in a conflict.
     * Verifies that the endpoint returns HTTP 409 Conflict and that the error message indicates the email duplication issue.
     *
     * @throws Exception if MockMvc request execution fails
     */
    @Test
    public void givenAdminLoggedIn_whenCreateUserWithExistingEmail_then422() throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setFirstName(ADMIN_CREATE_FIRST_NAME);
        dto.setLastName(ADMIN_CREATE_LAST_NAME);
        dto.setEmail(USER_AUTH_EMAIL);
        dto.setRole(UserRole.ROLE_USER);

        MvcResult mvcResult = this.mockMvc.perform(post(ADMIN_CREATE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dto))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();

        assertAll(
            () -> assertEquals(HttpStatus.UNPROCESSABLE_CONTENT.value(), mvcResult.getResponse().getStatus()),
            () -> assertTrue(mvcResult.getResponse().getContentAsString().contains("email"))
        );
    }

    /**
     * Tests that a non-admin user cannot create a new user.
     * Verifies that the endpoint returns HTTP 403 Forbidden when a regular user attempts to access the user creation functionality.
     *
     * @throws Exception if MockMvc request execution fails
     */
    @Test
    public void givenUserLoggedIn_whenCreateUser_then403() throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setFirstName(ADMIN_CREATE_FIRST_NAME);
        dto.setLastName(ADMIN_CREATE_LAST_NAME);
        dto.setEmail(ADMIN_CREATE_EMAIL);
        dto.setRole(UserRole.ROLE_USER);

        MvcResult mvcResult = this.mockMvc.perform(post(ADMIN_CREATE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dto))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(DEFAULT_USER, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus());
    }

    /**
     * Tests that a locked user with a valid JWT token cannot access protected endpoints.
     * Verifies that the JWT filter rejects the request with 401 even though the token is valid.
     */
    @Test
    public void givenLockedUser_whenAccessProtectedEndpointWithValidJwt_then401() throws Exception {
        // Lock the user in the DB
        testUser.setLocked(true);
        userRepository.save(testUser);

        MvcResult mvcResult = this.mockMvc.perform(
                MockMvcRequestBuilders.get(BASE_URI + "/users/me")
                    .header(securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.UNAUTHORIZED.value(), mvcResult.getResponse().getStatus());
    }

    /**
     * Tests that after an account is locked, a previously valid JWT is immediately invalidated.
     * Simulates a session that was active before the lock was applied.
     */
    @Test
    public void givenUserWithValidJwt_whenAccountBecomesLocked_thenSubsequentRequestIs401() throws Exception {
        MvcResult loginResult = this.mockMvc.perform(post(AUTH_BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(
                    UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
                        .withEmail(USER_AUTH_EMAIL)
                        .withPassword(USER_AUTH_PASSWORD)
                        .build())))
            .andReturn();

        assertEquals(HttpStatus.OK.value(), loginResult.getResponse().getStatus());
        String token = loginResult.getResponse().getContentAsString();

        MvcResult beforeLock = this.mockMvc.perform(
                MockMvcRequestBuilders.get(BASE_URI + "/users/me")
                    .header("Authorization", "Bearer " + token))
            .andDo(print())
            .andReturn();
        assertEquals(HttpStatus.OK.value(), beforeLock.getResponse().getStatus());

        testUser.setLocked(true);
        userRepository.save(testUser);

        MvcResult afterLock = this.mockMvc.perform(
                MockMvcRequestBuilders.get(BASE_URI + "/users/me")
                    .header("Authorization", "Bearer " + token))
            .andDo(print())
            .andReturn();
        assertEquals(HttpStatus.UNAUTHORIZED.value(), afterLock.getResponse().getStatus());
    }


    @Test
    public void givenLoggedInUser_whenGetMe_then200AndCorrectData() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(BASE_URI + "/users/me")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertTrue(response.getContentAsString().contains(USER_AUTH_FIRST_NAME)),
            () -> assertTrue(response.getContentAsString().contains(USER_AUTH_LAST_NAME)),
            () -> assertTrue(response.getContentAsString().contains(USER_AUTH_EMAIL))
        );
    }

    @Test
    public void givenExistingUser_whenRequestPasswordReset_then200() throws Exception {
        PasswordResetRequestDto dto = new PasswordResetRequestDto();
        dto.setEmail(USER_AUTH_EMAIL);

        MvcResult mvcResult = this.mockMvc.perform(post(PASSWORD_RESET_REQUEST_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dto)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    public void givenValidToken_whenConfirmPasswordReset_then200() throws Exception {
        String tokenHash = sha256Hex(TEST_RAW_TOKEN);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(testUser);
        resetToken.setTokenHash(tokenHash);
        resetToken.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(50));
        resetToken.setUsedAt(null);
        passwordResetTokenRepository.save(resetToken);

        PasswordResetConfirmDto confirmDto = new PasswordResetConfirmDto();
        confirmDto.setToken(TEST_RAW_TOKEN);
        confirmDto.setPassword(NEW_PASSWORD);

        MvcResult mvcResult = this.mockMvc.perform(post(PASSWORD_RESET_CONFIRM_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(confirmDto)))
            .andDo(print())
            .andReturn();

        ApplicationUser updatedUser = userRepository.findByEmail(USER_AUTH_EMAIL).orElseThrow();

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus()),
            () -> assertTrue(passwordEncoder.matches(NEW_PASSWORD, updatedUser.getPassword())),
            () -> assertFalse(updatedUser.getLocked()),
            () -> assertEquals(0, updatedUser.getFailedLoginAttempts())
        );
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    @Test
    public void givenLockedUser_whenAccountUnlocked_thenAccessRestoredWith200() throws Exception {
        testUser.setLocked(true);
        userRepository.save(testUser);

        MvcResult lockedResult = this.mockMvc.perform(
                MockMvcRequestBuilders.get(BASE_URI + "/users/me")
                    .header(securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();
        assertEquals(HttpStatus.UNAUTHORIZED.value(), lockedResult.getResponse().getStatus());

        testUser.setLocked(false);
        userRepository.save(testUser);

        MvcResult unlockedResult = this.mockMvc.perform(
                MockMvcRequestBuilders.get(BASE_URI + "/users/me")
                    .header(securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();
        assertEquals(HttpStatus.OK.value(), unlockedResult.getResponse().getStatus());
    }

}





