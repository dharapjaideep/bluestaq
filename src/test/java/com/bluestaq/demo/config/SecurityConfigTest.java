package com.bluestaq.demo.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Security rules for the web UI and the shape of 401 responses, through the real filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void webUiPageIsPublic() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Register account")));
    }

    @Test
    void rootIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyGetOfThePageIsPublic() throws Exception {
        mockMvc.perform(post("/index.html"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherPathsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/other.html"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCredentialsGet401WithoutBrowserLoginPrompt() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void wrongCredentialsGet401WithoutBrowserLoginPrompt() throws Exception {
        mockMvc.perform(get("/auth/me").with(httpBasic("nobody@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }
}
