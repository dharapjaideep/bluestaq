package com.bluestaq.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluestaq.demo.model.Team;
import com.bluestaq.demo.model.User;
import com.bluestaq.demo.repository.NoteRepository;
import com.bluestaq.demo.repository.TeamRepository;
import com.bluestaq.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end tests for {@code /notes} through the real security filter chain and in-memory
 * database. Teams are seeded directly through the repositories to keep these tests independent
 * of the team endpoints (covered by {@link TeamControllerTest}).
 *
 * <p>Cast: alice and bob are members of team "Alpha"; carol is in no team.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NoteControllerTest {

    private static final String PASSWORD = "correct-horse";
    private static final String ALICE = "alice@example.com";
    private static final String BOB = "bob@example.com";
    private static final String CAROL = "carol@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long alphaTeamId;

    @BeforeEach
    void seed() {
        clearAll();
        User alice = createUser(ALICE);
        User bob = createUser(BOB);
        createUser(CAROL);

        Team alpha = new Team("Alpha", alice);
        alpha.addMember(alice);
        alpha.addMember(bob);
        alphaTeamId = teamRepository.save(alpha).getId();
    }

    // Also clean up afterwards: other test classes share this database and delete users, which
    // the foreign keys from notes and teams would otherwise block.
    @AfterEach
    void clearAll() {
        noteRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- owner CRUD ---

    @Test
    void ownerCanCreateReadUpdateAndDeletePersonalNote() throws Exception {
        long id = createNote(ALICE, "Groceries", "milk, eggs", null);

        perform(get("/notes/" + id), ALICE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Groceries"))
                .andExpect(jsonPath("$.content").value("milk, eggs"))
                .andExpect(jsonPath("$.teamId").doesNotExist())
                .andExpect(jsonPath("$.ownerId").value(userId(ALICE)));

        perform(put("/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Groceries v2", "milk, eggs, bread", null)), ALICE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Groceries v2"))
                .andExpect(jsonPath("$.content").value("milk, eggs, bread"));

        perform(delete("/notes/" + id), ALICE).andExpect(status().isNoContent());
        perform(get("/notes/" + id), ALICE).andExpect(status().isNotFound());
    }

    @Test
    void createReturns201WithGeneratedFields() throws Exception {
        perform(post("/notes").contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Plan", "body", alphaTeamId)), ALICE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.teamId").value(alphaTeamId))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void ownerCanMoveNoteIntoAndOutOfTheirTeam() throws Exception {
        long id = createNote(ALICE, "Draft", null, null);

        perform(put("/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Draft", null, alphaTeamId)), ALICE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(alphaTeamId));

        perform(put("/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Draft", null, null)), ALICE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").doesNotExist());
    }

    // --- team members ---

    @Test
    void teamMemberCanReadAndEditTeamNoteButNotDeleteIt() throws Exception {
        long id = createNote(ALICE, "Sprint plan", "v1", alphaTeamId);

        perform(get("/notes/" + id), BOB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sprint plan"));

        perform(put("/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Sprint plan", "v2 by bob", alphaTeamId)), BOB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("v2 by bob"))
                // Editing does not transfer ownership.
                .andExpect(jsonPath("$.ownerId").value(userId(ALICE)));

        perform(delete("/notes/" + id), BOB)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        assertThat(noteRepository.existsById(id)).isTrue();
    }

    @Test
    void teamMemberCannotMoveTeamNote() throws Exception {
        long id = createNote(ALICE, "Sprint plan", null, alphaTeamId);

        // Omitting teamId on PUT means "make it personal" - an owner-only move.
        perform(put("/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Sprint plan", null, null)), BOB)
                .andExpect(status().isForbidden());

        perform(get("/notes/" + id), ALICE).andExpect(jsonPath("$.teamId").value(alphaTeamId));
    }

    // --- no access -> 404 ---

    @Test
    void nonMemberGets404OnTeamNote() throws Exception {
        long id = createNote(ALICE, "Sprint plan", null, alphaTeamId);

        perform(get("/notes/" + id), CAROL).andExpect(status().isNotFound());
        perform(put("/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("hijacked", null, alphaTeamId)), CAROL)
                .andExpect(status().isNotFound());
        perform(delete("/notes/" + id), CAROL).andExpect(status().isNotFound());

        assertThat(noteRepository.findById(id).orElseThrow().getTitle()).isEqualTo("Sprint plan");
    }

    @Test
    void anotherUsersPersonalNoteIs404EvenForTeammates() throws Exception {
        long id = createNote(ALICE, "Private", null, null);

        for (String other : new String[] {BOB, CAROL}) {
            perform(get("/notes/" + id), other)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Note not found"));
            perform(delete("/notes/" + id), other).andExpect(status().isNotFound());
        }
        assertThat(noteRepository.existsById(id)).isTrue();
    }

    @Test
    void missingNoteIs404LikeAnInaccessibleOne() throws Exception {
        perform(get("/notes/999999"), ALICE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Note not found"));
    }

    // --- team assignment ---

    @Test
    void creatingNoteInTeamYouAreNotInReturns400() throws Exception {
        perform(post("/notes").contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Sneaky", null, alphaTeamId)), CAROL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.teamId").exists());

        assertThat(noteRepository.count()).isZero();
    }

    @Test
    void nonExistentTeamGivesSameErrorAsTeamYouAreNotIn() throws Exception {
        perform(post("/notes").contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Sneaky", null, 999999L)), CAROL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.teamId").value("You are not a member of this team"));
    }

    @Test
    void movingOwnNoteIntoTeamYouAreNotInReturns400() throws Exception {
        long id = createNote(CAROL, "Mine", null, null);

        perform(put("/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("Mine", null, alphaTeamId)), CAROL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.teamId").exists());

        assertThat(noteRepository.findById(id).orElseThrow().getTeamId()).isNull();
    }

    // --- listing and search ---

    @Test
    void listReturnsOnlyAccessibleNotes() throws Exception {
        createNote(ALICE, "Alice personal", null, null);
        createNote(ALICE, "Alpha shared", null, alphaTeamId);
        createNote(BOB, "Bob personal", null, null);
        createNote(CAROL, "Carol personal", null, null);

        perform(get("/notes"), BOB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Alpha shared", "Bob personal")));

        perform(get("/notes"), CAROL)
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Carol personal")));
    }

    @Test
    void listCanFilterByTeam() throws Exception {
        createNote(ALICE, "Alice personal", null, null);
        createNote(ALICE, "Alpha shared", null, alphaTeamId);

        perform(get("/notes").param("teamId", alphaTeamId.toString()), ALICE)
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Alpha shared")));

        // A team filter cannot widen access: carol sees nothing from Alpha.
        perform(get("/notes").param("teamId", alphaTeamId.toString()), CAROL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchIsCaseInsensitiveOnTitleAndContent() throws Exception {
        createNote(ALICE, "Quarterly REPORT", null, null);
        createNote(ALICE, "Misc", "draft of the report appendix", null);
        createNote(ALICE, "Shopping", "milk", null);

        perform(get("/notes").param("q", "Report"), ALICE)
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Quarterly REPORT", "Misc")));
    }

    @Test
    void searchTreatsLikeWildcardsLiterally() throws Exception {
        createNote(ALICE, "100% done", null, null);
        createNote(ALICE, "1000 items", null, null);

        perform(get("/notes").param("q", "100%"), ALICE)
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("100% done")));
    }

    // --- validation and auth ---

    @Test
    void blankTitleReturns400() throws Exception {
        perform(post("/notes").contentType(MediaType.APPLICATION_JSON)
                .content(noteJson(" ", "body", null)), ALICE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void notesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/notes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/notes").contentType(MediaType.APPLICATION_JSON)
                .content(noteJson("x", null, null)))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private User createUser(String email) {
        return userRepository.save(new User(email, passwordEncoder.encode(PASSWORD)));
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String email) throws Exception {
        return mockMvc.perform(request.with(httpBasic(email, PASSWORD)));
    }

    private long createNote(String email, String title, String content, Long teamId) throws Exception {
        String body = perform(post("/notes").contentType(MediaType.APPLICATION_JSON)
                .content(noteJson(title, content, teamId)), email)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String noteJson(String title, String content, Long teamId) {
        return "{\"title\":" + jsonString(title)
                + ",\"content\":" + jsonString(content)
                + ",\"teamId\":" + teamId + "}";
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
