package com.bluestaq.demo.exception;

/**
 * Thrown when a team does not exist or the caller is not a member of it; mapped to 404 Not
 * Found. The two cases share one exception so non-members cannot probe which team ids exist.
 */
public class TeamNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a client-safe message.
     */
    public TeamNotFoundException() {
        super("Team not found");
    }
}
