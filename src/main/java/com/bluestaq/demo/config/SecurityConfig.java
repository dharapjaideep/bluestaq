package com.bluestaq.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * HTTP security configuration: stateless HTTP Basic authentication for the JSON API.
 *
 * <p>Users are loaded by {@link com.bluestaq.demo.service.AppUserDetailsService}; passwords are
 * checked with the {@link PasswordEncoder} defined here.
 */
@Configuration
public class SecurityConfig {

    /**
     * Defines which endpoints are public and how callers authenticate.
     *
     * @param http the security builder provided by Spring Security
     * @return the configured filter chain
     * @throws Exception if the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Security: CSRF protection is disabled because this is a stateless JSON API.
                // Credentials travel in the Authorization header on every request, never in a
                // cookie the browser would attach automatically, so there is nothing to forge.
                .csrf(csrf -> csrf.disable())
                // Security: no HTTP session; every request must carry its own credentials.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Security: registration must be public (callers have no account yet).
                        // Only POST is opened; other methods on this path still require auth.
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        // Security: the single-page web UI must load before the user can log in.
                        // Only GET of this one static page is public; it contains no data, and
                        // every API call it makes still goes through the rules here.
                        .requestMatchers(HttpMethod.GET, "/", "/index.html").permitAll()
                        // Security: TEMPORARY - the H2 console is public for local development.
                        // It has its own DB login, but must be removed before any real deployment.
                        .requestMatchers("/h2-console/**").permitAll()
                        // Fail safe: anything not listed above requires an authenticated user.
                        .anyRequest().authenticated())
                // Missing or wrong credentials get a plain 401: no redirect to a login page, and
                // no "WWW-Authenticate: Basic" header. That header makes browsers pop up their
                // own login dialog over the web UI; the page shows its own error instead.
                // Clients that send credentials up front (curl -u, the web UI) are unaffected.
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Security: the H2 console renders in frames; allow framing by this origin only,
                // which still blocks clickjacking from other sites.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /**
     * Password hasher used for registration and login.
     *
     * @return a BCrypt encoder (salted, deliberately slow hashing)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
