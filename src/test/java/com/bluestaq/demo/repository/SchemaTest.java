package com.bluestaq.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that tests run against the schema from {@code schema.sql}, with Hibernate only
 * validating it, and that the database itself enforces the users constraints.
 */
@SpringBootTest
class SchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void hibernateOnlyValidatesTheSchema() {
        assertThat(ddlAuto).isEqualTo("validate");
    }

    @Test
    void usersTableHasTheNamedUniqueConstraintFromSchemaSql() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                        + "WHERE TABLE_NAME = 'USERS' AND CONSTRAINT_NAME = 'UK_USERS_EMAIL'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void databaseRejectsDuplicateEmail() {
        insertUser("dup@example.com");

        assertThatThrownBy(() -> insertUser("dup@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsNullPasswordHash() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, created_at) VALUES (?, NULL, CURRENT_TIMESTAMP)",
                "nohash@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertUser(String email) {
        jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, created_at) VALUES (?, 'hash', CURRENT_TIMESTAMP)",
                email);
    }
}
