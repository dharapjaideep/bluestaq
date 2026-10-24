package com.bluestaq.demo.exception;

import com.bluestaq.demo.dto.ApiError;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    /**
     * A note that does not exist or that the caller cannot access.
     *
     * @param e the not-found exception
     * @return 404 Not Found
     */
    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<ApiError> handleNoteNotFound(NoteNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), Map.of());
    }

    /**
     * An owner-only action on a note attempted by a team member.
     *
     * @param e the owner-only exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(NoteOwnerOnlyException.class)
    public ResponseEntity<ApiError> handleOwnerOnly(NoteOwnerOnlyException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage(), Map.of());
    }

    /**
     * A note assigned to a team the caller is not a member of.
     *
     * @param e the invalid-team exception
     * @return 400 with the error on the {@code teamId} field, like a validation failure
     */
    @ExceptionHandler(InvalidTeamException.class)
    public ResponseEntity<ApiError> handleInvalidTeam(InvalidTeamException e) {
        return error(HttpStatus.BAD_REQUEST, "Validation failed", Map.of("teamId", e.getMessage()));
    }

    /**
     * A note update based on an out-of-date version.
     *
     * @param e the version-conflict exception
     * @return 409 Conflict asking the client to reload
     */
    @ExceptionHandler(NoteVersionConflictException.class)
    public ResponseEntity<ApiError> handleVersionConflict(NoteVersionConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    /**
     * Two saves of the same entity racing: the later flush finds the row's version already
     * moved on. Only notes are versioned, so the note message applies.
     *
     * @param e the optimistic-locking exception
     * @return 409 Conflict asking the client to reload
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        return error(HttpStatus.CONFLICT, NoteVersionConflictException.MESSAGE, Map.of());
    }

    /**
     * A team or user that does not exist, or that the caller cannot see.
     *
     * @param e the not-found exception
     * @return 404 Not Found
     */
    @ExceptionHandler({TeamNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ApiError> handleTeamOrUserNotFound(RuntimeException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), Map.of());
    }

    /**
     * A creator-only team action attempted by another member.
     *
     * @param e the creator-only exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(TeamCreatorOnlyException.class)
    public ResponseEntity<ApiError> handleCreatorOnly(TeamCreatorOnlyException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage(), Map.of());
    }

    /**
     * A membership change that conflicts with the team's state.
     *
     * @param e the conflict exception
     * @return 409 Conflict
     */
    @ExceptionHandler(TeamMembershipConflictException.class)
    public ResponseEntity<ApiError> handleMembershipConflict(TeamMembershipConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    /**
     * A path or query parameter of the wrong type, e.g. {@code /notes/abc}.
     *
     * @param e the type-mismatch exception
     * @return 400 naming the bad parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'", Map.of());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), message, fieldErrors));
    }
}
