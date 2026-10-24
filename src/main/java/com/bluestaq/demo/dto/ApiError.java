package com.bluestaq.demo.dto;

import java.util.Map;

/**
 * JSON error body returned by {@link com.bluestaq.demo.exception.ApiExceptionHandler}.
 *
 * @param status      the HTTP status code
 * @param error       the HTTP reason phrase
 * @param message     a client-safe description
 * @param fieldErrors per-field validation messages; empty when not a validation error
 */
public record ApiError(int status, String error, String message, Map<String, String> fieldErrors) {
}
