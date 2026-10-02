package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PasswordResetConfirmDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PasswordResetRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.UserMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.service.PasswordResetService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordResetService passwordResetService;

    public UserEndpoint(UserService userService, UserMapper userMapper, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.passwordResetService = passwordResetService;
    }

    @PermitAll
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registers a new user in the system.")
    public void register(@Valid @RequestBody UserRegisterDto userRegisterDto) throws ValidationException {
        LOGGER.info("POST /api/v1/users/register body: {}", userRegisterDto);
        userService.register(userRegisterDto);
    }

    @PermitAll
    @PostMapping("/authentication")
    @Operation(summary = "User Authentification")
    public String login(@Valid @RequestBody UserLoginDto userLoginDto) {
        LOGGER.info("POST /api/v1/users/login body: {}", userLoginDto);
        return userService.login(userLoginDto);
    }

    @PermitAll
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Requests a password reset email.")
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto passwordResetRequestDto, HttpServletRequest request) {
        LOGGER.info("POST /api/v1/users/password-reset/request for {}", passwordResetRequestDto.getEmail());
        passwordResetService.requestPasswordReset(passwordResetRequestDto.getEmail(), resolveFrontendOrigin(request));
    }

    @PermitAll
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Resets a password using a reset token.")
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDto passwordResetConfirmDto) throws ValidationException {
        LOGGER.info("POST /api/v1/users/password-reset/confirm");
        passwordResetService.resetPassword(passwordResetConfirmDto.getToken(), passwordResetConfirmDto.getPassword());
    }


    @Secured("ROLE_ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a new user (Admin only)", security = @SecurityRequirement(name = "apiKey"))
    public void createUser(@Valid @RequestBody UserCreateDto userCreateDto) throws ValidationException {
        LOGGER.info("POST /api/v1/users - Admin creating user with email: {}", userCreateDto.getEmail());
        userService.createUser(userCreateDto);
    }

    @Secured("ROLE_ADMIN")
    @PatchMapping("/{id}/locked")
    @Operation(summary = "Lock or unlock a user (Admin only)", security = @SecurityRequirement(name = "apiKey"))
    public void setUserLockedStatus(@PathVariable("id") Long id, @RequestBody Boolean locked) throws ValidationException {
        LOGGER.info("PATCH /api/v1/users/{}/locked - New status: {}", id, locked);
        userService.setLockedStatus(id, locked);
    }

    @Secured("ROLE_ADMIN")
    @GetMapping
    @Operation(summary = "Get all users (Admin only)", security = @SecurityRequirement(name = "apiKey"))
    public List<ApplicationUser> getAll() {
        LOGGER.info("GET /api/v1/users");
        return userService.findAll();
    }


    @PutMapping(value = "/me", produces = "text/plain")
    @Operation(summary = "Updating Userdata")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String update(Authentication auth, @Valid @RequestBody UserUpdateDto userUpdateDto) throws ValidationException {
        LOGGER.info("PUT /api/v1/users/me body: {}", userUpdateDto);
        return userService.update(auth.getName(), userUpdateDto);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deleting Userdata")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public void delete(Authentication auth) {
        LOGGER.info("DELETE /api/v1/users/me");

        String email = auth.getName();
        userService.deleteByEmail(email);
    }

    @GetMapping("/me")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Information about the loged in user ")
    public UserUpdateDto getCurrentUser(Authentication authentication) {
        ApplicationUser applicationUser = userService.findApplicationUserByEmail(authentication.getName());
        return userMapper.applicationUserToUserUpdateDto(applicationUser);
    }

    private String resolveFrontendOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return trimTrailingSlash(origin);
        }

        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }

        try {
            URI refererUri = new URI(referer);
            if (refererUri.getScheme() == null || refererUri.getAuthority() == null) {
                return null;
            }
            return trimTrailingSlash(refererUri.getScheme() + "://" + refererUri.getAuthority());
        } catch (URISyntaxException e) {
            LOGGER.debug("Could not parse Referer header '{}'", referer, e);
            return null;
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

}
