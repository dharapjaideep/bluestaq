package com.bluestaq.demo.exception;

/**
 * Thrown when a team member who is not the team's creator attempts a creator-only action, such
 * as removing another member; mapped to 403 Forbidden. 403 rather than 404 is safe here because
 * the caller is a member and can already see the team.
 */
public class TeamCreatorOnlyException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message a client-safe description of the refused action
     */
    public TeamCreatorOnlyException(String message) {
        super(message);
    }
}
