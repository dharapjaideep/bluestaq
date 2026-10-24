package com.bluestaq.demo.exception;

/**
 * Thrown when a membership change conflicts with the team's current state: adding someone who
 * is already a member, or removing the team's creator. Mapped to 409 Conflict.
 */
public class TeamMembershipConflictException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message a client-safe description of the conflict
     */
    public TeamMembershipConflictException(String message) {
        super(message);
    }
}
