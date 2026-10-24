package com.bluestaq.demo.service;

import com.bluestaq.demo.dto.RegisterRequest;
import com.bluestaq.demo.dto.UserResponse;
import com.bluestaq.demo.exception.EmailAlreadyRegisteredException;
import com.bluestaq.demo.model.User;
import com.bluestaq.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and current-user lookup.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user with a hashed password.
     *
     * @param request the validated registration request
     * @return the created user (without the password hash)
     * @throws EmailAlreadyRegisteredException if the email is already in use (case-insensitive)
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        try {
            // saveAndFlush so the unique constraint is checked here, not at commit time.
            User saved = userRepository.saveAndFlush(new User(email, passwordEncoder.encode(request.password())));
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations can both pass existsByEmail; the database's unique
            // constraint is the real guard, so the losing request is reported as a duplicate too.
            throw new EmailAlreadyRegisteredException();
        }
    }

    /**
     * Looks up an authenticated user by email.
     *
     * @param email the authenticated principal's email
     * @return the user's public view
     * @throws IllegalStateException if the user no longer exists
     */
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(User.normalizeEmail(email))
                .map(UserResponse::from)
                // Should not happen: HTTP Basic loaded this same user earlier in this request.
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
