package com.bluestaq.demo.exception;

/**
 * Thrown when registering an email that already has an account; mapped to 409 Conflict.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    /**
     * Creates the exception with a client-safe message.
     */
    public EmailAlreadyRegisteredException() {
        super("Email is already registered");
    }
}
