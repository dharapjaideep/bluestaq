package com.bluestaq.demo.service;

import com.bluestaq.demo.dto.NoteRequest;
import com.bluestaq.demo.dto.NoteResponse;
import com.bluestaq.demo.exception.InvalidTeamException;
import com.bluestaq.demo.exception.NoteNotFoundException;
import com.bluestaq.demo.exception.NoteOwnerOnlyException;
import com.bluestaq.demo.model.Note;
import com.bluestaq.demo.model.Team;
import com.bluestaq.demo.model.User;
import com.bluestaq.demo.repository.NoteRepository;
import com.bluestaq.demo.repository.TeamRepository;
import com.bluestaq.demo.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note CRUD and search, with all access rules enforced here:
 * <ul>
 *   <li>A user can read and edit a note they own, or one in a team they are a member of.</li>
 *   <li>Only the owner can delete a note or move it to a different team (403 for members).</li>
 *   <li>A note can only be assigned to a team the user is a member of (400).</li>
 *   <li>Without access, the note is reported as not found (404), never as forbidden, so
 *       callers cannot probe which note ids exist.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    /**
     * Creates a note owned by the caller.
     *
     * @param email   the authenticated caller's email
     * @param request the validated note fields
     * @return the created note
     * @throws InvalidTeamException if {@code teamId} is set and the caller is not in that team
     */
    @Transactional
    public NoteResponse create(String email, NoteRequest request) {
        User user = currentUser(email);
        Team team = request.teamId() == null ? null : teamForMember(request.teamId(), user.getId());
        Note note = noteRepository.save(new Note(request.title(), request.content(), user, team));
        return NoteResponse.from(note);
    }

    /**
     * Lists the notes the caller can access, most recently updated first.
     *
     * @param email  the authenticated caller's email
     * @param teamId only notes in this team, or {@code null} for all accessible notes
     * @param query  case-insensitive text to find in title or content, or {@code null}/blank for no filter
     * @return the matching notes; empty if a team filter names a team the caller cannot see
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> list(String email, Long teamId, String query) {
        User user = currentUser(email);
        return noteRepository.findAccessible(user.getId(), teamId, likePattern(query)).stream()
                .map(NoteResponse::from)
                .toList();
    }

    /**
     * Returns one note.
     *
     * @param email the authenticated caller's email
     * @param id    the note's id
     * @return the note
     * @throws NoteNotFoundException if it does not exist or the caller cannot access it
     */
    @Transactional(readOnly = true)
    public NoteResponse get(String email, Long id) {
        return NoteResponse.from(accessibleNote(id, currentUser(email)));
    }

    /**
     * Replaces a note's title, content and team.
     *
     * @param email   the authenticated caller's email
     * @param id      the note's id
     * @param request the validated new fields; a {@code null} {@code teamId} makes the note personal
     * @return the updated note
     * @throws NoteNotFoundException  if it does not exist or the caller cannot access it
     * @throws NoteOwnerOnlyException if a non-owner tries to change the note's team
     * @throws InvalidTeamException   if the new team is one the caller is not a member of
     */
    @Transactional
    public NoteResponse update(String email, Long id, NoteRequest request) {
        User user = currentUser(email);
        Note note = accessibleNote(id, user);

        Team team = note.getTeam();
        // Membership is only checked when the team changes: an owner who has since left the
        // note's team can still edit the note without being forced to move it.
        if (!Objects.equals(request.teamId(), note.getTeamId())) {
            if (!note.isOwnedBy(user.getId())) {
                throw new NoteOwnerOnlyException("Only the note's owner can move it to a different team");
            }
            team = request.teamId() == null ? null : teamForMember(request.teamId(), user.getId());
        }

        note.update(request.title(), request.content(), team);
        return NoteResponse.from(note);
    }

    /**
     * Deletes a note.
     *
     * @param email the authenticated caller's email
     * @param id    the note's id
     * @throws NoteNotFoundException  if it does not exist or the caller cannot access it
     * @throws NoteOwnerOnlyException if the caller is a team member but not the owner
     */
    @Transactional
    public void delete(String email, Long id) {
        User user = currentUser(email);
        Note note = accessibleNote(id, user);
        if (!note.isOwnedBy(user.getId())) {
            throw new NoteOwnerOnlyException("Only the note's owner can delete it");
        }
        noteRepository.delete(note);
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(User.normalizeEmail(email))
                // Should not happen: HTTP Basic loaded this same user earlier in this request.
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private Note accessibleNote(Long id, User user) {
        // Security: the access rule is part of the query, so "no access" and "does not exist"
        // are indistinguishable to the caller (both 404).
        return noteRepository.findAccessible(id, user.getId()).orElseThrow(NoteNotFoundException::new);
    }

    private Team teamForMember(Long teamId, Long userId) {
        // Security: a non-existent team and a team the user is not in give the same error, so
        // team ids cannot be probed.
        if (!teamRepository.isMember(teamId, userId)) {
            throw new InvalidTeamException();
        }
        return teamRepository.getReferenceById(teamId);
    }

    /**
     * Builds a lower-case "contains" LIKE pattern, escaping the LIKE wildcards in the user's text
     * so that e.g. a search for "100%" matches literally.
     */
    private static String likePattern(String query) {
        if (query == null || query.isBlank()) {
            return "%";
        }
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
