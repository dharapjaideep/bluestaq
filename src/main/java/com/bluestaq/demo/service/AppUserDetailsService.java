package com.bluestaq.demo.service;

import com.bluestaq.demo.model.User;
import com.bluestaq.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users for Spring Security authentication. The "username" is the user's email,
 * matched case-insensitively.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by email for password verification.
     *
     * @param username the email the client sent, in any case
     * @return the user's security details (normalised email, password hash, role)
     * @throws UsernameNotFoundException if no user has this email
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(User.normalizeEmail(username))
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .roles("USER")
                        .build())
                // Security: Spring Security reports this as the same generic 401 as a wrong
                // password, so callers cannot tell an unknown email from a bad password.
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
