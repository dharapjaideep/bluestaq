package com.bluestaq.demo.exception;

/**
 * Thrown when a note update is based on an out-of-date version, i.e. someone else saved the
 * note after the caller read it; mapped to 409 Conflict.
 */
public class NoteVersionConflictException extends RuntimeException {

    /** Client-facing message, shared with the handler for concurrent-save failures. */
    public static final String MESSAGE =
            "This note was changed by someone else. Reload to get the latest version.";

    /**
     * Creates the exception with the client-facing message.
     */
    public NoteVersionConflictException() {
        super(MESSAGE);
    }
}
