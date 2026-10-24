package com.bluestaq.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /notes}. Updates use {@link NoteUpdateRequest}, which also carries the
 * note's version.
 *
 * @param title   the note's title; required, at most 255 characters
 * @param content the note's body; optional, at most 20000 characters
 * @param teamId  the team to share the note with, or {@code null} for a personal note
 */
public record NoteRequest(
        // Sizes match the column lengths in schema.sql, so over-long input is a clean 400
        // rather than a database error.
        @NotBlank @Size(max = 255) String title,
        @Size(max = 20000) String content,
        Long teamId) {
}
