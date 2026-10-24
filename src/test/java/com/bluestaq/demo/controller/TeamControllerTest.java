package com.bluestaq.demo.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * End-to-end tests for {@code /teams} through the real security filter chain and in-memory
 * database, including the effect of membership changes on access to team notes.
 *
 * <p>Cast: alice creates teams; bob and carol start in no team.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TeamControllerTest {

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

    @BeforeEach
    void seed() {
        clearAll();
        createUser(ALICE);
        createUser(BOB);
        createUser(CAROL);
    }

    // Also clean up afterwards: other test classes share this database and delete users, which
    // the foreign keys from notes and teams would otherwise block.
    @AfterEach
    void clearAll() {
        noteRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- create and list ---

    @Test
    void createTeamMakesCreatorTheFirstMember() throws Exception {
        perform(post("/teams").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Engineering\"}"), ALICE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.createdById").value(userId(ALICE)))
                .andExpect(jsonPath("$.members[*].email", contains(ALICE)));
    }

    @Test
    void blankTeamNameReturns400() throws Exception {
        perform(post("/teams").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"), ALICE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void listReturnsOnlyMyTeamsWithMemberEmails() throws Exception {
        long engineering = createTeam(ALICE, "Engineering");
        addMember(ALICE, engineering, BOB).andExpect(status().isCreated());
        createTeam(ALICE, "Alice solo");
        createTeam(CAROL, "Carol's team");

        perform(get("/teams"), BOB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Engineering"))
                .andExpect(jsonPath("$[0].members[*].email", contains(ALICE, BOB)));

        perform(get("/teams"), ALICE)
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Engineering", "Alice solo")));
    }

    // --- add members ---

    @Test
    void addedMemberCanReadTeamNotes() throws Exception {
        long team = createTeam(ALICE, "Engineering");
        long noteId = createTeamNote(ALICE, team);

        perform(get("/notes/" + noteId), BOB).andExpect(status().isNotFound());

        addMember(ALICE, team, BOB)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.members[*].email", contains(ALICE, BOB)));

        perform(get("/notes/" + noteId), BOB).andExpect(status().isOk());
    }

    @Test
    void anyMemberCanAddMembers() throws Exception {
        long team = createTeam(ALICE, "Engineering");
        addMember(ALICE, team, BOB).andExpect(status().isCreated());

        addMember(BOB, team, CAROL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.members[*].email", contains(ALICE, BOB, CAROL)));
    }

    @Test
    void addMemberMatchesEmailCaseInsensitively() throws Exception {
        long team = createTeam(ALICE, "Engineering");

        addMember(ALICE, team, "BOB@Example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.members[*].email", contains(ALICE, BOB)));
    }

    @Test
    void addUnknownEmailReturns404() throws Exception {
        long team = createTeam(ALICE, "Engineering");

        addMember(ALICE, team, "nobody@example.com")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No user with this email"));
    }

    @Test
    void addExistingMemberReturns409() throws Exception {
        long team = createTeam(ALICE, "Engineering");
        addMember(ALICE, team, BOB).andExpect(status().isCreated());

        addMember(ALICE, team, BOB).andExpect(status().isConflict());
        addMember(ALICE, team, ALICE).andExpect(status().isConflict());
    }

    // --- non-members ---

    @Test
    void nonMemberGets404OnAllTeamEndpoints() throws Exception {
        long team = createTeam(ALICE, "Engineering");
        addMember(ALICE, team, BOB).andExpect(status().isCreated());

        addMember(CAROL, team, CAROL)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Team not found"));
        perform(delete("/teams/" + team + "/members/" + userId(BOB)), CAROL)
                .andExpect(status().isNotFound());
        perform(get("/notes").param("teamId", String.valueOf(team)), CAROL)
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void missingTeamGivesSameResponseAsTeamYouAreNotIn() throws Exception {
        addMember(ALICE, 999999L, BOB)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Team not found"));
    }

    // --- remove members ---

    @Test
    void removedMemberLosesAccessToTeamNotesImmediately() throws Exception {
        long team = createTeam(ALICE, "Engineering");
        addMember(ALICE, team, BOB).andExpect(status().isCreated());
        long noteId = createTeamNote(ALICE, team);
        perform(get("/notes/" + noteId), BOB).andExpect(status().isOk());

        perform(delete("/teams/" + team + "/members/" + userId(BOB)), ALICE)
                .andExpect(status().isNoContent());

        perform(get("/notes/" + noteId), BOB).andExpect(status().isNotFound());
        perform(get("/notes"), BOB).andExpect(jsonPath("$", hasSize(0)));
        perform(get("/teams"), BOB).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void memberCanRemoveThemselves() throws Exception {
        long team = createTeam(ALICE, "Engineering");
        addMember(ALICE, team, BOB).andExpect(status().isCreated());

        perform(delete("/teams/" + team + "/members/" + userId(BOB)), BOB)
                .andExpect(status().isNoContent());

        perform(get("/teams"), ALICE)
                .andExpect(jsonPath("$[0].members[*].email", contains(ALICE)));
    }

    @Test
    void nonCreatorCannotRemoveOthers() throws Exception {
        long team = createTeam(ALICE, "Engineering");
        addMember(ALICE, team, BOB).andExpect(status().isCreated());
        addMember(ALICE, team, CAROL).andExpect(status().isCreated());

        perform(delete("/teams/" + team + "/members/" + userId(CAROL)), BOB)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        perform(delete("/teams/" + team + "/members/" + userId(ALICE)), BOB)
                .andExpect(status().isForbidden());

        perform(get("/teams"), ALICE)
                .andExpect(jsonPath("$[0].members[*].email", contains(ALICE, BOB, CAROL)));
    }

    @Test
    void creatorCannotBeRemoved() throws Exception {
        long team = createTeam(ALICE, "Engineering");

        perform(delete("/teams/" + team + "/members/" + userId(ALICE)), ALICE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The team's creator can't be removed"));
    }

    @Test
    void removingANonMemberReturns404() throws Exception {
        long team = createTeam(ALICE, "Engineering");

        perform(delete("/teams/" + team + "/members/" + userId(CAROL)), ALICE)
                .andExpect(status().isNotFound());
    }

    @Test
    void teamsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/teams")).andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private void createUser(String email) {
        userRepository.save(new User(email, passwordEncoder.encode(PASSWORD)));
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String email) throws Exception {
        return mockMvc.perform(request.with(httpBasic(email, PASSWORD)));
    }

    private long createTeam(String email, String name) throws Exception {
        String body = perform(post("/teams").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"), email)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions addMember(String actor, long teamId, String email) throws Exception {
        return perform(post("/teams/" + teamId + "/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"), actor);
    }

    private long createTeamNote(String email, long teamId) throws Exception {
        String body = perform(post("/notes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Team note\",\"teamId\":" + teamId + "}"), email)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
