package com.bluestaq.demo.exception;

/**
 * Thrown when a team operation names a user who does not exist, or who is not in the team;
 * mapped to 404 Not Found.
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message a client-safe description
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
