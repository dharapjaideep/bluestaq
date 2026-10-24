package com.bluestaq.demo.repository;

import com.bluestaq.demo.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link User}. Callers must pass emails already normalised with
 * {@link User#normalizeEmail(String)}.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by normalised email.
     *
     * @param email the normalised email
     * @return the user, or empty if none exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a normalised email is already registered.
     *
     * @param email the normalised email
     * @return {@code true} if a user with this email exists
     */
    boolean existsByEmail(String email);
}
