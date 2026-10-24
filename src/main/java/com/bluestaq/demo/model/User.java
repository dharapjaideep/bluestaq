package com.bluestaq.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A registered user account. Emails are stored normalised (trimmed, lower-case) so that
 * lookups are case-insensitive.
 */
@Entity
// "user" is a reserved word in H2, hence the plural table name.
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA only
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Creates a new, unsaved user.
     *
     * @param email        the user's email; normalised before it is stored
     * @param passwordHash the already-hashed password (never the raw password)
     */
    public User(String email, String passwordHash) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
    }

    /**
     * Normalises an email for storage and lookup: trims whitespace and lower-cases it.
     *
     * @param email the raw email, may be {@code null}
     * @return the normalised email, or {@code null} if the input was {@code null}
     */
    public static String normalizeEmail(String email) {
        // Locale.ROOT so the result does not depend on the server's locale
        // (e.g. a Turkish locale would lower-case "I" to a dotless "ı").
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
