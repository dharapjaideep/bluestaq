package com.bluestaq.demo.exception;

/**
 * Thrown when a note is assigned to a team the caller is not a member of (or that does not
 * exist); mapped to 400 Bad Request on the {@code teamId} field. Both cases share one message so
 * the response does not reveal which team ids exist.
 */
public class InvalidTeamException extends RuntimeException {

    /**
     * Creates the exception with a client-safe message.
     */
    public InvalidTeamException() {
        super("You are not a member of this team");
    }
}
