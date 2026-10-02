package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.PasswordResetToken;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.PasswordResetTokenRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.PasswordResetServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.UserValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidator userValidator;

    private PasswordResetServiceImpl passwordResetService;

    private static final String FRONTEND_ORIGIN = "http://localhost:4200";

    private ApplicationUser testUser;
    private PasswordResetToken testToken;

    @BeforeEach
    public void beforeEach() {
        passwordResetService = new PasswordResetServiceImpl(
            userRepository, passwordResetTokenRepository, emailService,
            passwordEncoder, userValidator,
            "http://localhost:4200", "/reset-password", 60);

        testUser = ApplicationUser.ApplicationUserBuilder.anApplicationUser()
            .withFirstName("Test")
            .withLastName("User")
            .withEmail("user@test.at")
            .withPassword("encodedPassword")
            .build();

        testToken = new PasswordResetToken();
        testToken.setUser(testUser);
        testToken.setTokenHash("abc123hash");
        testToken.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        testToken.setExpiresAt(LocalDateTime.now().plusMinutes(50));
    }

    @Test
    public void requestPasswordReset_whenEmailExists_createsTokenAndSendsEmail() {
        when(userRepository.findByEmail("user@test.at")).thenReturn(Optional.of(testUser));

        passwordResetService.requestPasswordReset("user@test.at", FRONTEND_ORIGIN);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass((Class) PasswordResetToken.class);

        assertAll(
            () -> verify(passwordResetTokenRepository).invalidateActiveTokensForUser(eq(testUser), any(LocalDateTime.class)),
            () -> verify(passwordResetTokenRepository).save(tokenCaptor.capture()),
            () -> verify(emailService).sendPasswordResetEmail(eq("user@test.at"), anyString()),
            () -> {
                PasswordResetToken savedToken = tokenCaptor.getValue();
                assertNotNull(savedToken.getTokenHash());
                assertNotNull(savedToken.getCreatedAt());
                assertNotNull(savedToken.getExpiresAt());
            }
        );
    }

    @Test
    public void requestPasswordReset_whenEmailNotExists_doesNothing() {
        when(userRepository.findByEmail("unknown@test.at")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("unknown@test.at", FRONTEND_ORIGIN);

        assertAll(
            () -> verify(passwordResetTokenRepository, never()).invalidateActiveTokensForUser(any(), any()),
            () -> verify(passwordResetTokenRepository, never()).save(any()),
            () -> verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString())
        );
    }

    @Test
    public void resetPassword_withValidToken_updatesPasswordAndInvalidatesToken() throws ValidationException {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode("NewStr0ng@Pass1")).thenReturn("newEncodedPassword");

        passwordResetService.resetPassword("some-raw-token", "NewStr0ng@Pass1");

        assertAll(
            () -> verify(userValidator).validatePassword("NewStr0ng@Pass1"),
            () -> verify(passwordEncoder).encode("NewStr0ng@Pass1"),
            () -> verify(userRepository).save(testUser),
            () -> assertEquals("newEncodedPassword", testUser.getPassword()),
            () -> assertEquals(0, testUser.getFailedLoginAttempts()),
            () -> assertEquals(false, testUser.getLocked()),
            () -> assertNotNull(testToken.getUsedAt()),
            () -> verify(passwordResetTokenRepository).save(testToken),
            () -> verify(passwordResetTokenRepository).invalidateActiveTokensForUser(eq(testUser), any(LocalDateTime.class))
        );
    }

    @Test
    public void resetPassword_withExpiredToken_throwsValidationException() {
        testToken.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(testToken));

        ValidationException exception = assertThrows(ValidationException.class,
            () -> passwordResetService.resetPassword("expired-token", "NewStr0ng@Pass1"));
        assertTrue(exception.errors().getFirst().contains("invalid or expired"));
    }
}
