package com.bluestaq.demo.controller;

import com.bluestaq.demo.dto.NoteRequest;
import com.bluestaq.demo.dto.NoteResponse;
import com.bluestaq.demo.dto.NoteUpdateRequest;
import com.bluestaq.demo.service.NoteService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Note endpoints. All require authentication (SecurityConfig's default rule). Thin delegator to
 * {@link NoteService}, which enforces the access rules.
 */
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * Creates a note owned by the caller.
     *
     * @param principal the authenticated caller
     * @param request   title, content and optional team id, validated
     * @return the created note
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@AuthenticationPrincipal UserDetails principal,
                               @Valid @RequestBody NoteRequest request) {
        return noteService.create(principal.getUsername(), request);
    }

    /**
     * Lists the notes the caller can access.
     *
     * @param principal the authenticated caller
     * @param teamId    optional team filter
     * @param q         optional case-insensitive search on title and content
     * @return the matching notes, most recently updated first
     */
    @GetMapping
    public List<NoteResponse> list(@AuthenticationPrincipal UserDetails principal,
                                   @RequestParam(required = false) Long teamId,
                                   @RequestParam(required = false) String q) {
        return noteService.list(principal.getUsername(), teamId, q);
    }

    /**
     * Returns one note.
     *
     * @param principal the authenticated caller
     * @param id        the note's id
     * @return the note
     */
    @GetMapping("/{id}")
    public NoteResponse get(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return noteService.get(principal.getUsername(), id);
    }

    /**
     * Replaces a note's title, content and team.
     *
     * @param principal the authenticated caller
     * @param id        the note's id
     * @param request   the new fields and the version they are based on, validated
     * @return the updated note, with its new version
     */
    @PutMapping("/{id}")
    public NoteResponse update(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id,
                               @Valid @RequestBody NoteUpdateRequest request) {
        return noteService.update(principal.getUsername(), id, request);
    }

    /**
     * Deletes a note. Owner only.
     *
     * @param principal the authenticated caller
     * @param id        the note's id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        noteService.delete(principal.getUsername(), id);
    }
}
