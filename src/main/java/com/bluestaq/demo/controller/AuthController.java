package com.bluestaq.demo.controller;

import com.bluestaq.demo.dto.RegisterRequest;
import com.bluestaq.demo.dto.UserResponse;
import com.bluestaq.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Thin delegator to {@link AuthService}.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user. Public endpoint (see SecurityConfig).
     *
     * @param request email and password, validated
     * @return the created user's id and email
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Returns the authenticated caller; clients use this to check their credentials.
     *
     * @param principal the user authenticated by HTTP Basic on this request
     * @return the caller's id and email
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails principal) {
        return authService.getByEmail(principal.getUsername());
    }
}
