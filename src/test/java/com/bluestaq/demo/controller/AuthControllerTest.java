package com.bluestaq.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluestaq.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end tests for {@code /auth/register} and {@code /auth/me} through the real
 * security filter chain and in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String EMAIL = "alice@example.com";
    private static final String PASSWORD = "correct-horse";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void registerReturns201WithIdAndEmailButNoPassword() throws Exception {
        register(EMAIL, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));

        // The password is stored hashed, never in plain text.
        String storedHash = userRepository.findByEmail(EMAIL).orElseThrow().getPasswordHash();
        assertThat(storedHash).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void registerNormalisesEmailToLowerCase() throws Exception {
        register("Alice@Example.COM", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        register(EMAIL, PASSWORD).andExpect(status().isCreated());

        register(EMAIL, "another-password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void duplicateEmailInDifferentCaseReturns409() throws Exception {
        register(EMAIL, PASSWORD).andExpect(status().isCreated());

        register("ALICE@example.com", PASSWORD).andExpect(status().isConflict());
    }

    @Test
    void invalidEmailReturns400() throws Exception {
        register("not-an-email", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").doesNotExist());
    }

    @Test
    void shortPasswordReturns400() throws Exception {
        register(EMAIL, "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.email").doesNotExist());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void passwordOfMaxLengthIsAccepted() throws Exception {
        String password = "a".repeat(72);
        register(EMAIL, password).andExpect(status().isCreated());

        mockMvc.perform(get("/auth/me").with(httpBasic(EMAIL, password)))
                .andExpect(status().isOk());
    }

    @Test
    void tooLongPasswordReturns400() throws Exception {
        register(EMAIL, "a".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void blankFieldsReturn400() throws Exception {
        register("", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void meWithoutCredentialsReturns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithCorrectCredentialsReturnsCurrentUser() throws Exception {
        register(EMAIL, PASSWORD).andExpect(status().isCreated());
        Long id = userRepository.findByEmail(EMAIL).orElseThrow().getId();

        mockMvc.perform(get("/auth/me").with(httpBasic(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meAcceptsEmailInAnyCase() throws Exception {
        register(EMAIL, PASSWORD).andExpect(status().isCreated());

        mockMvc.perform(get("/auth/me").with(httpBasic("ALICE@Example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void meWithWrongPasswordReturns401() throws Exception {
        register(EMAIL, PASSWORD).andExpect(status().isCreated());

        mockMvc.perform(get("/auth/me").with(httpBasic(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithUnknownUserReturns401() throws Exception {
        mockMvc.perform(get("/auth/me").with(httpBasic("nobody@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions register(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
