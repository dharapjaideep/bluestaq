package com.bluestaq.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluestaq.demo.config.DemoDataSeeder;
import com.bluestaq.demo.model.User;
import com.bluestaq.demo.repository.NoteRepository;
import com.bluestaq.demo.repository.TeamRepository;
import com.bluestaq.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests the demo data seeding and that the startup seeder is switched off for tests.
 */
@SpringBootTest
class DemoDataServiceTest {

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationContext context;

    // Clean before and after: other test classes share this database.
    @BeforeEach
    @AfterEach
    void clearAll() {
        noteRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void seederIsNotActiveInTests() {
        assertThat(context.getBeansOfType(DemoDataSeeder.class)).isEmpty();
    }

    @Test
    void seedsUsersTeamAndNotesIntoEmptyDatabase() {
        assertThat(demoDataService.seedIfEmpty()).isTrue();

        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(teamRepository.count()).isEqualTo(1);
        assertThat(noteRepository.count()).isEqualTo(5);

        // Passwords are stored hashed and match the documented demo password.
        User alice = userRepository.findByEmail("alice@demo.com").orElseThrow();
        assertThat(alice.getPasswordHash()).isNotEqualTo(DemoDataService.DEMO_PASSWORD);
        assertThat(passwordEncoder.matches(DemoDataService.DEMO_PASSWORD, alice.getPasswordHash())).isTrue();

        Long teamId = teamRepository.findAll().getFirst().getId();
        Long bobId = userRepository.findByEmail("bob@demo.com").orElseThrow().getId();
        Long carolId = userRepository.findByEmail("carol@demo.com").orElseThrow().getId();
        assertThat(teamRepository.isMember(teamId, alice.getId())).isTrue();
        assertThat(teamRepository.isMember(teamId, bobId)).isTrue();
        assertThat(teamRepository.isMember(teamId, carolId)).isFalse();
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        userRepository.save(new User("existing@example.com", passwordEncoder.encode("whatever1")));

        assertThat(demoDataService.seedIfEmpty()).isFalse();

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(teamRepository.count()).isZero();
        assertThat(noteRepository.count()).isZero();
    }
}
