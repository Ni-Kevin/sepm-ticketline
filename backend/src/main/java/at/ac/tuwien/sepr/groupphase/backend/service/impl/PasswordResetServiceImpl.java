package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.PasswordResetToken;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.PasswordResetTokenRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import at.ac.tuwien.sepr.groupphase.backend.service.PasswordResetService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String frontendBaseUrl;
    private final String frontendResetPath;
    private final long tokenTtlMinutes;

    public PasswordResetServiceImpl(UserRepository userRepository,
                                    PasswordResetTokenRepository passwordResetTokenRepository,
                                    EmailService emailService,
                                    PasswordEncoder passwordEncoder,
                                    UserValidator userValidator,
                                    @Value("${app.password-reset.frontend-base-url}") String frontendBaseUrl,
                                    @Value("${app.password-reset.frontend-reset-path}") String frontendResetPath,
                                    @Value("${app.password-reset.token-ttl-minutes}") long tokenTtlMinutes) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
        this.frontendBaseUrl = frontendBaseUrl;
        this.frontendResetPath = frontendResetPath;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email, String frontendOrigin) {
        userRepository.findByEmail(email).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            String rawToken = generateToken();

            passwordResetTokenRepository.invalidateActiveTokensForUser(user, now);

            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setUser(user);
            passwordResetToken.setTokenHash(hashToken(rawToken));
            passwordResetToken.setCreatedAt(now);
            passwordResetToken.setExpiresAt(now.plusMinutes(tokenTtlMinutes));
            passwordResetTokenRepository.save(passwordResetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), buildResetLink(rawToken, frontendOrigin));
            LOGGER.info("Created password reset token for user {}", user.getEmail());
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) throws ValidationException {
        userValidator.validatePassword(newPassword);

        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByTokenHash(hashToken(token))
            .orElseThrow(() -> new ValidationException("Validation failed", List.of("Password reset token is invalid or expired")));

        LocalDateTime now = LocalDateTime.now();
        if (passwordResetToken.getUsedAt() != null || passwordResetToken.getExpiresAt().isBefore(now)) {
            throw new ValidationException("Validation failed", List.of("Password reset token is invalid or expired"));
        }

        ApplicationUser user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        userRepository.save(user);

        passwordResetToken.setUsedAt(now);
        passwordResetTokenRepository.save(passwordResetToken);
        passwordResetTokenRepository.invalidateActiveTokensForUser(user, now);

        LOGGER.info("Password reset completed for user {}", user.getEmail());
    }

    private String buildResetLink(String token, String frontendOrigin) {
        String baseUrl = frontendOrigin == null || frontendOrigin.isBlank()
            ? frontendBaseUrl
            : trimTrailingSlash(frontendOrigin) + frontendResetPath;
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "token=" + token;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
