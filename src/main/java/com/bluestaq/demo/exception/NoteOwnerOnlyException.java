package com.bluestaq.demo.exception;

/**
 * Thrown when a user who can access a note (a team member) attempts an owner-only action, such
 * as deleting it or moving it to another team; mapped to 403 Forbidden. 403 rather than 404 is
 * safe here because the caller can already see the note.
 */
public class NoteOwnerOnlyException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message a client-safe description of the refused action
     */
    public NoteOwnerOnlyException(String message) {
        super(message);
    }
}
