package com.bluestaq.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /auth/register}.
 *
 * @param email    the new account's email
 * @param password the raw password; 8 to 72 characters
 */
public record RegisterRequest(
        @Email @NotBlank String email,
        // max = 72: BCrypt only uses the first 72 bytes of a password, so longer input would be
        // silently truncated (or rejected by the encoder); reject it up front with a clear 400.
        @NotBlank @Size(min = 8, max = 72) String password) {
}
