package com.bluestaq.demo.dto;

import com.bluestaq.demo.model.Note;
import java.time.Instant;

/**
 * A note as returned by the API.
 *
 * @param id        the note's id
 * @param title     the title
 * @param content   the body, may be {@code null}
 * @param ownerId   the id of the user who owns the note
 * @param teamId    the note's team, or {@code null} for a personal note
 * @param createdAt when the note was created
 * @param updatedAt when the note was last changed
 */
public record NoteResponse(Long id, String title, String content, Long ownerId, Long teamId,
                           Instant createdAt, Instant updatedAt) {

    /**
     * Builds the API view of a note.
     *
     * @param note the entity
     * @return the response DTO
     */
    public static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(),
                note.getOwner().getId(), note.getTeamId(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
