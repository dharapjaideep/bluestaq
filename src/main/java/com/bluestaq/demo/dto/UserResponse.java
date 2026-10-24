package com.bluestaq.demo.dto;

import com.bluestaq.demo.model.User;

/**
 * Public view of a user returned by the API. Deliberately excludes the password hash.
 *
 * @param id    the user's id
 * @param email the user's normalised email
 */
public record UserResponse(Long id, String email) {

    /**
     * Builds the public view of a user.
     *
     * @param user the entity
     * @return the response DTO
     */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}
