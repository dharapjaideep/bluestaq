package com.bluestaq.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /notes/{id}}. PUT replaces the whole note, so an omitted {@code teamId}
 * makes the note personal.
 *
 * @param title   the note's title; required, at most 255 characters
 * @param content the note's body; optional, at most 20000 characters
 * @param teamId  the team to share the note with, or {@code null} for a personal note
 * @param version the version of the note this edit is based on (from the last read); required
 *                so an edit made on stale data is rejected with 409 instead of overwriting
 */
public record NoteUpdateRequest(
        // Sizes match the column lengths in schema.sql, as in NoteRequest.
        @NotBlank @Size(max = 255) String title,
        @Size(max = 20000) String content,
        Long teamId,
        @NotNull Long version) {
}
