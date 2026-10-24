package com.bluestaq.demo.exception;

import com.bluestaq.demo.dto.ApiError;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns known exceptions into clean JSON {@link ApiError} responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Bean Validation failures on a request body.
     *
     * @param e the validation exception
     * @return 400 with one message per invalid field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        // TreeMap for a stable field order; if a field has several violations, keep the first.
        Map<String, String> fieldErrors = new TreeMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    /**
     * Missing or malformed JSON body.
     *
     * @param e the parse exception
     * @return 400 without echoing parser internals
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request body", Map.of());
    }

    /**
     * Registration with an email that already exists.
     *
     * @param e the conflict exception
     * @return 409 Conflict
     */
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleConflict(EmailAlreadyRegisteredException e) {
        return error(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), message, fieldErrors));
    }
}
