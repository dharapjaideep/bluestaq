package com.bluestaq.demo.exception;

/**
 * Thrown when a note does not exist or the caller cannot access it; mapped to 404 Not Found.
 * The two cases share one exception so responses never reveal that someone else's note exists.
 */
public class NoteNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a client-safe message.
     */
    public NoteNotFoundException() {
        super("Note not found");
    }
}
