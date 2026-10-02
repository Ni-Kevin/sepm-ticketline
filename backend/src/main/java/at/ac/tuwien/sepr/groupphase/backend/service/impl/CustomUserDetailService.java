package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.UserMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class CustomUserDetailService implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenizer jwtTokenizer;
    private final UserValidator validator;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final SeatRepository seatRepository;

    @Autowired
    public CustomUserDetailService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   JwtTokenizer jwtTokenizer, UserValidator validator, UserMapper userMapper, EmailService emailService, SeatRepository seatRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenizer = jwtTokenizer;
        this.validator = validator;
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.seatRepository = seatRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        LOGGER.debug("Load all user by email");
        try {
            ApplicationUser applicationUser = findApplicationUserByEmail(email);

            List<GrantedAuthority> grantedAuthorities;
            if (applicationUser.getRole() == UserRole.ROLE_ADMIN) {
                grantedAuthorities = AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_USER");
            } else {
                grantedAuthorities = AuthorityUtils.createAuthorityList("ROLE_USER");
            }

            return new User(applicationUser.getEmail(), applicationUser.getPassword(),
                true, true, true, !applicationUser.getLocked(), grantedAuthorities);
        } catch (NotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        }
    }

    @Override
    public ApplicationUser findApplicationUserByEmail(String email) {
        LOGGER.debug("Find application user by email");
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(
                String.format("Invalid email or password")
            ));
    }

    @Override
    public String login(UserLoginDto userLoginDto) {
        ApplicationUser applicationUser;

        try {
            applicationUser = findApplicationUserByEmail(userLoginDto.getEmail());
        } catch (NotFoundException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (applicationUser.getLocked()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (passwordEncoder.matches(userLoginDto.getPassword(), applicationUser.getPassword())) {
            applicationUser.setFailedLoginAttempts(0);
            userRepository.save(applicationUser);
            return jwtTokenizer.getAuthToken(applicationUser.getEmail(), getRolesAsList(applicationUser));

        } else {
            int newAttempts = applicationUser.getFailedLoginAttempts() + 1;
            applicationUser.setFailedLoginAttempts(newAttempts);

            if (newAttempts >= 5 && applicationUser.getRole() == UserRole.ROLE_USER) {
                applicationUser.setLocked(true);
            }
            userRepository.save(applicationUser);
            throw new BadCredentialsException("Invalid email or password.");
        }
    }

    private List<String> getRolesAsList(ApplicationUser user) {
        return user.getRole() == UserRole.ROLE_ADMIN
            ? List.of("ROLE_ADMIN", "ROLE_USER")
            : List.of("ROLE_USER");
    }


    public void register(UserRegisterDto userRegisterDto) throws ValidationException {
        validator.validateForRegistration(userRegisterDto);
        ApplicationUser userToRegister = userMapper.userRegisterDtoToApplicationUser(userRegisterDto);
        userToRegister.setPassword(passwordEncoder.encode(userRegisterDto.getPassword()));
        userRepository.save(userToRegister);
    }

    @Override
    @Transactional
    public String update(String currentEmail, UserUpdateDto userUpdateDto) throws ValidationException {
        LOGGER.info("Updating user with email: {}", currentEmail);

        validator.validateForUpdate(currentEmail, userUpdateDto);

        ApplicationUser user = userRepository.findByEmail(currentEmail)
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (userUpdateDto.getPassword() != null && !userUpdateDto.getPassword().isBlank()) {

            if (userUpdateDto.getOldPassword() == null || !passwordEncoder.matches(userUpdateDto.getOldPassword(), user.getPassword())) {
                throw new ValidationException("Update failed", List.of("Current password provided is incorrect."));
            }
            user.setPassword(passwordEncoder.encode(userUpdateDto.getPassword()));
        }


        user.setEmail(userUpdateDto.getEmail());
        user.setFirstName(userUpdateDto.getFirstName());
        user.setLastName(userUpdateDto.getLastName());

        userRepository.save(user);

        return jwtTokenizer.getAuthToken(user.getEmail(), List.of(user.getRole().name()));
    }

    @Override
    @Transactional
    public void deleteByEmail(String emailToDelete) {
        LOGGER.debug("deleteByEmail({})", emailToDelete);

        ApplicationUser userToDelete = userRepository.findByEmail(emailToDelete)
            .orElseThrow(() -> new NotFoundException("No user with this email"));

        ApplicationUser anonymousUser = userRepository.findByEmail("deleted@system.local")
            .orElseGet(() -> {
                LOGGER.info("Create Dummy for deleted users with tickets");
                ApplicationUser dummy = new ApplicationUser();
                dummy.setEmail("deleted@system.local");
                dummy.setFirstName("Deleted");
                dummy.setLastName("User");
                dummy.setPassword("SYSTEM_ACCOUNT_NO_LOGIN_" + java.util.UUID.randomUUID());
                dummy.setRole(UserRole.ROLE_USER);
                dummy.setLocked(true);
                return userRepository.save(dummy);
            });

        if (userToDelete.getPurchaseOrders() != null) {
            for (Order order : userToDelete.getPurchaseOrders()) {
                order.setUser(anonymousUser);
            }
        }

        if (userToDelete.getReservations() != null) {
            for (Reservation reservation : userToDelete.getReservations()) {
                reservation.setUser(anonymousUser);
            }
        }

        userToDelete.getPurchaseOrders().clear();
        userToDelete.getReservations().clear();

        userRepository.delete(userToDelete);
    }

    public void createUser(UserCreateDto userCreateDto) throws ValidationException {
        validator.validateForCreation(userCreateDto);

        String rawPassword = generateSecureInitialPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        ApplicationUser userToCreate = userMapper.userCreateDtoToApplicationUser(userCreateDto);
        userToCreate.setPassword(encodedPassword);
        userRepository.save(userToCreate);

        emailService.sendInitialPasswordEmail(userToCreate.getEmail(), userToCreate.getFirstName(), rawPassword);

        LOGGER.info("Created user with email {} and generated an initial password", userCreateDto.getEmail());
    }

    public void setLockedStatus(Long id, Boolean locked) throws NotFoundException, ValidationException {
        ApplicationUser user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));

        String currentAdminEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Long currentAdminId = userRepository.findByEmail(currentAdminEmail)
            .map(ApplicationUser::getId)
            .orElseThrow(() -> new NotFoundException("Authenticated admin not found"));
        //TODO: Check if implementing a Conflict Exception would be better
        if (currentAdminId.equals(user.getId())) {
            throw new ValidationException("Safety Error", List.of("You cannot lock or unlock your own account."));
        }
        user.setLocked(locked);

        if (!locked) {
            user.setFailedLoginAttempts(0);
        }

        userRepository.save(user);
    }

    @Override
    public List<ApplicationUser> findAll() {
        LOGGER.info("Fetching all users from the database");
        return userRepository.findAll();
    }

    private String generateSecureInitialPassword() {
        final String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        final String digits = "0123456789";
        final String specialCharacters = "!@#$%^&*()-_+=<>?";
        final String allChars = upperCase + lowerCase + digits + specialCharacters;
        final int passwordLength = 12;

        SecureRandom random = new SecureRandom();
        List<Character> passwordChars = new ArrayList<>();

        passwordChars.add(upperCase.charAt(random.nextInt(upperCase.length())));
        passwordChars.add(lowerCase.charAt(random.nextInt(lowerCase.length())));
        passwordChars.add(digits.charAt(random.nextInt(digits.length())));
        passwordChars.add(specialCharacters.charAt(random.nextInt(specialCharacters.length())));

        for (int i = 4; i < passwordLength; i++) {
            passwordChars.add(allChars.charAt(random.nextInt(allChars.length())));
        }
        Collections.shuffle(passwordChars, random);

        StringBuilder password = new StringBuilder();
        for (char c : passwordChars) {
            password.append(c);
        }
        return password.toString();
    }
}
