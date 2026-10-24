package com.bluestaq.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /teams/{id}/members}.
 *
 * @param email the email of an existing user to add, in any case
 */
public record AddMemberRequest(@Email @NotBlank String email) {
}
