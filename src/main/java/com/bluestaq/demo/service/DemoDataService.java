package com.bluestaq.demo.service;

import com.bluestaq.demo.model.Note;
import com.bluestaq.demo.model.Team;
import com.bluestaq.demo.model.User;
import com.bluestaq.demo.repository.NoteRepository;
import com.bluestaq.demo.repository.TeamRepository;
import com.bluestaq.demo.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the demo users, team and notes listed in README.md, so the API can be tried out
 * straight after startup. Called by {@link com.bluestaq.demo.config.DemoDataSeeder}.
 */
@Service
@RequiredArgsConstructor
public class DemoDataService {

    /** Password shared by all demo accounts; documented in README.md. */
    public static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final NoteRepository noteRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Seeds the demo data, but only into an empty database: if any user exists, nothing is
     * changed, so real or previously seeded data is never touched or duplicated.
     *
     * @return {@code true} if data was seeded, {@code false} if the database already had users
     */
    @Transactional
    public boolean seedIfEmpty() {
        if (userRepository.count() > 0) {
            return false;
        }

        User alice = createUser("alice@demo.com");
        User bob = createUser("bob@demo.com");
        User carol = createUser("carol@demo.com");

        Team engineering = new Team("Engineering", alice);
        engineering.addMember(alice);
        engineering.addMember(bob);
        teamRepository.save(engineering);

        noteRepository.saveAll(List.of(
                new Note("Alice's todo", "Book dentist appointment; renew passport.", alice, null),
                new Note("Bob's reading list", "Designing Data-Intensive Applications.", bob, null),
                new Note("Carol's ideas", "Dark mode for the notes UI.", carol, null),
                new Note("Sprint 12 plan", "Ship the notes API; start on team endpoints.", alice, engineering),
                new Note("On-call runbook", "Restart the service, then check the H2 console.", alice, engineering)));
        return true;
    }

    private User createUser(String email) {
        return userRepository.save(new User(email, passwordEncoder.encode(DEMO_PASSWORD)));
    }
}
