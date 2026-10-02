package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//TODO: replace this class with a correct ApplicationUser JPARepository implementation
@Repository
public interface UserRepository extends JpaRepository<ApplicationUser, Long> {

    /**
     * Find a user by their email address.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found, or empty if not
     */
    Optional<ApplicationUser> findByEmail(String email);

    /**
     * Check if a user with the given email already exists.
     *
     * @param email the email to check
     * @return true if a user exists, false otherwise
     */
    boolean existsByEmail(String email);


}
